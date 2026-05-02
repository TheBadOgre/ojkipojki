# ojkipojki

Simplified tabletop simulator. Lets you load sprite sheets, spawn tokens on a board, and move them. Think Tabletop Simulator but minimal.

## Current state

`Main.kt` opens `LauncherWindow`. From there, user connects to a server or hosts one. Full Swing UI is implemented.

## Architecture

Client-server over TCP sockets. Server owns all mutable state. Client holds a read-only mirror of that state updated via events.

```
Client                          Server
  |                               |
  |-- Command (TCP) ------------> |
  |                         handle command (serialized via commandExecutor)
  |                         update ModelRepository
  |                               |
  | <-- Event (TCP, broadcast) -- |
  update StateRepository          |
```

### Server (`server/`)

- **`ModelRepository`** — in-memory store (`ConcurrentHashMap`) for mutable model objects: `SpriteBagModel`, `SpriteModel`, `TokenModel`.
- **`CommandDispatcher`** — routes incoming commands to their handler by type.
- **`commandExecutor`** (`ServerContext`) — single-threaded `ExecutorService`; all command dispatch runs through it to prevent concurrent model mutation.
- **`EventBroadcastService`** — sends an event to all connected clients. `broadcastCustom(factory)` overload lets one event vary per recipient (used for `PointersUpdatedEvent`).
- **`ClientSessionManager`** / **`ConnectionManager`** — manage TCP connections and per-client sessions.
- **`AutoSaveService`** — saves `ModelRepository` to `last_game.sav` every 5 minutes via a daemon `Timer`.
- **`TokenSyncHeartbeatService`** — broadcasts a full `TokensSyncEvent` to all clients every 5 seconds.
- **`GameLoader`** / **`GamePersistence`** — load/save game state using Java object serialization. `GameLoader.tryLoad` returns `false` if the save file is corrupted (repo is cleared; caller surfaces the error).
- **`ClientColorRegistry`** — assigns a distinct RGB color from a pool of 32 to each connected client; used for pointer rendering.
- **`PointerRepository`** — in-memory `ConcurrentHashMap<clientId, Pointer>` for current pointer positions; `findAllExcept(clientId)` is used for broadcasting.
- **`CommandContext`** — `ThreadLocal<String?>` holding the client ID of the command currently being dispatched. Set per-task by the executor before calling `dispatch`.

Models (`server/model/`) are mutable classes with `apply(domain)` to absorb domain state and `toState()` to produce domain objects for events. Sub-models: `PositionModel`, `RotationModel`, `IndexModel`.

### Client (`client/`)

- **`StateRepository`** — in-memory store (`ConcurrentHashMap`) for immutable domain objects: `SpriteBag`, `Sprite`, `Token`, `Pointer`.
- **`EventDispatcher`** — routes incoming events to their handler by type.
- **`CommandTransmitter`** — sends commands to the server over the socket.
- **`SpriteLoader`** — loads sprite bags from a directory of PNGs (see below).
- **`SpriteBagDirectoryLoader`** — scans `sprites/` subdirectories, calls `SpriteLoader` per subdir. Also exposes `getFolderStructure()` (name → `SpriteBagId` list) for the UI tree without loading images.
- **`ApplicationHandler`** — wires `ClientContext` callbacks to UI, opens `MainWindow`, uploads local sprites on connect, shows disconnect dialog.
- **`ClientContext`** — global service locator with `stateRepository`, `eventDispatcher`, and nullable callbacks (`onTokensUpdated`, `onSpriteBagsUpdated`, `onPointersUpdated`, `onConnectedClientsUpdated`).

### Shared (`shared/`)

Domain classes and protocol interfaces used by both sides.

**Domain objects** (immutable, used as-is on client):
- `SpriteBag(id, groupName, sprites)` — a named collection of sprites; `groupName` is the subdirectory name.
- `Sprite(id, frontImageBytes, backImageBytes)` — one game piece, front and back images as PNG bytes.
- `Token(id, spriteId, position, rotation, index, flipped, locked)` — a placed instance of a sprite on the board.
- `Pointer(x, y, red, green, blue)` — another client's cursor position, identified by its assigned RGB color.

**IDs:**
- `SpriteBagId(String)` — simple string name (e.g. `"dice"`).
- `SpriteId(SpriteBagId, red, green, blue)` — composite key; the RGB comes from the mask file (see SpriteLoader).
- `TokenId(UUID)` — random UUID assigned at spawn.

**Protocol abstractions:** `Handler<A>`, `Dispatcher<A>`, `Receiver`, `Transmitter` — generic interfaces for the command/event pipeline.

**`LocaleService`** — loads `/locale/locale_en.properties` from classpath, then overlays the system language file if different from `en`. `get(key)` / `get(key, vararg args)` for parameterised strings. Loaded once at startup.

## Commands and Events

| Direction | Type | Effect |
|---|---|---|
| Client → Server | `UploadSpriteBagsCommand(spriteBags)` | Upsert sprite bags in `ModelRepository`, broadcast `SpriteBagsUpdatedEvent` |
| Client → Server | `SpawnTokensCommand(spriteBagId, position?, spriteId?)` | Create `TokenModel` per sprite (or one if `spriteId` given), broadcast `SomeTokensUpdatedEvent` |
| Client → Server | `MoveTokensCommand(adjustments)` | Update position/rotation/index/flipped on existing tokens; null field = no change; broadcast `SomeTokensUpdatedEvent` |
| Client → Server | `DeleteTokensCommand(tokenIds)` | Remove tokens, broadcast `SomeTokensUpdatedEvent` |
| Client → Server | `ShuffleTokensCommand(tokenIds)` | Randomise `index` values of given tokens, broadcast `SomeTokensUpdatedEvent` |
| Client → Server | `LockTokensCommand(tokenIds, locked)` | Set `locked` flag on tokens, broadcast `SomeTokensUpdatedEvent` |
| Client → Server | `MovePointerCommand(x, y)` | Store pointer in `PointerRepository`, broadcastCustom `PointersUpdatedEvent` (each client gets list excluding itself) |
| Server → Clients | `SpriteBagsUpdatedEvent(spriteBags)` | Full list of all sprite bags — client saves each, no purge |
| Server → Clients | `SomeTokensUpdatedEvent(tokenActions)` | Per-token Update/Delete delta — client applies each action individually |
| Server → Clients | `TokensSyncEvent(tokens)` | Full token list — client replaces all. Sent on connect and every 5 s by heartbeat |
| Server → Clients | `PointersUpdatedEvent(pointers)` | All other clients' current pointer positions |
| Server → Clients | `ConnectedClientsUpdateEvent(numOfClients)` | Current session count |

## Sprite loading (`SpriteLoader`)

Reads a directory of PNGs. Files are grouped by the prefix before the first `_`. Each group needs three files:

```
{id}_front.png   — front face of all sprites in this bag
{id}_back.png    — back face
{id}_mask.png    — color map: each unique non-black RGB color defines one sprite
```

`SpriteId` encodes the RGB from the mask. The loader finds the bounding box of each color region, crops it out of front/back images, and masks pixels belonging to other sprites transparent.

## Package structure

```
net.rafkos.ojkipojki
├── launcher/
│   ├── LauncherWindow.kt      # Entry point UI — connect or host
│   └── ServerConsoleWindow.kt # Dark log window shown when hosting; tees stdout/stderr
├── shared/
│   ├── domain/          # Domain classes + IDs (immutable, Serializable, all have serialVersionUID)
│   ├── locale/          # LocaleService
│   └── protocol/        # Handler, Dispatcher, Receiver, Transmitter + command/event types
├── server/
│   ├── ServerContext.kt # Global service locator (modelRepository, commandDispatcher, commandExecutor, …)
│   ├── ServerRunner.kt  # Wires up server, starts services, registers shutdown hook
│   ├── model/           # Mutable model classes (apply/toState): SpriteBagModel, SpriteModel, TokenModel, sub-models
│   ├── application/     # ModelRepository, PointerRepository, ClientColorRegistry,
│   │                    # GameLoader, GamePersistence, AutoSaveService, TokenSyncHeartbeatService,
│   │                    # CommandContext
│   └── protocol/
│       ├── command/     # CommandDispatcher, CommandReceiver + handlers
│       └── event/       # EventBroadcastService, EventTransmitter
└── client/
    ├── ClientContext.kt # Global service locator + event callbacks
    ├── ClientRunner.kt  # Wires up client, opens MainWindow via ApplicationHandler
    ├── application/     # StateRepository, SpriteLoader, SpriteBagDirectoryLoader
    ├── protocol/
    │   ├── ApplicationHandler.kt  # Connects network session to UI lifecycle
    │   ├── command/     # CommandTransmitter
    │   └── event/       # EventDispatcher + handlers
    └── view/
        ├── MainWindow.kt
        ├── state/       # SelectionState, ViewportState, TokenAnimator, PointerAnimator
        ├── action/      # BoardActions, CommandDebouncer, SpriteBagSpawnHandler, PointerCommandSender
        ├── render/      # TokenRenderer
        ├── input/       # BoardMouseController, BoardWheelController, DragRectOverlay
        ├── icon/        # Icons (toolbar icon helpers)
        └── panel/       # BoardPanel, ToolbarPanel, SpriteBagListPanel, StatusBarPanel
```

## Launcher (`launcher/`)

`LauncherWindow` — two panels:
- **Connect to server**: host + port fields (default `127.0.0.1:12001`), "connect to server" button → `ClientRunner.startClient(host, port)` on daemon thread, launcher disposes.
- **Host server**: port field (default `12001`), "host server" button → opens `ServerConsoleWindow`, then `ServerRunner.startServer(port)` on daemon thread, launcher disposes.

Closing launcher via X exits JVM (nothing else running). After a button click launcher swaps to `DISPOSE_ON_CLOSE` before disposing, so the JVM stays alive.

`ServerConsoleWindow` — dark monospace JFrame titled "Ojkipojki server". On creation tees `System.out`/`System.err` to its `JTextArea`; restores original streams on close.

## UI architecture (`client/view/`)

Swing-based UI. All business state in `state/`, rendering in `render/`, input handling in `input/`, no logic in panels.

### Threading rules
- All Swing work on EDT. Network events arrive on socket thread → handlers dispatch via `SwingUtilities.invokeLater`.
- `StateRepository` is thread-safe (`ConcurrentHashMap`). Reads on EDT are fine.
- `TokenAnimator`, `PointerAnimator`, `SelectionState`, `ViewportState` are EDT-only — no sync needed.

### Coordinate system (`ViewportState`)
- `screen = (world + offset) * zoom + panelCenter`
- `worldToScreen` / `screenToWorld` for hit-testing and DnD drops.
- `affineTransform(w, h)`: `translate(panelCenter) * scale(zoom) * translate(offset)` — applied via `g2.transform(at)` (concatenates, preserves HiDPI base transform).

### Rendering (`BoardPanel.paintComponent`)
1. Apply viewport transform → draw grid in world space.
2. Sort tokens by `index.value` ascending (lowest = bottom).
3. Each token: `tokenAnimator.visualize(token)` for smooth position, then `TokenRenderer.draw()`.
4. Draw other clients' pointers using `pointerAnimator.visualize(pointer)`.
5. Restore transform → draw drag-rect overlay in screen space.

### Token depth effect (`TokenRenderer`)
Three layers per token, drawn in world space under the token's local transform:
1. Shadow at `(+5, +5)`: alpha-mask of image, pure black, 45% opacity.
2. Dark edge at `(+2, +2)`: image darkened to 35% brightness — simulates thickness.
3. Main image at `(0, 0)`.
All three images cached per `SpriteId`. Shadow/edge computed once from front image (not per flipped state).

### Selection (`SelectionState`)
- `Set<TokenId>` + listener list. All mutation methods call `notifyListeners()`.
- `pruneAgainst(tokens)` called on every token update event to drop deleted token IDs.
- Selection outline: 1px blue (#50A0FF) dashed stroke, width corrected to `1/zoom` to stay 1px on screen.

### Hit testing (`BoardMouseController.findTokenAt`)
Sort tokens `sortedBy { index }.reversed()` — same order as rendering but reversed, so topmost visible token is checked first.

### Smooth animation (`TokenAnimator`, `PointerAnimator`)
Both share the same lerp pattern (factor 0.25):
- `syncWithXxx(list)`: called on network events (EDT). New entries initialise at target (no jump); existing entries keep current visual state and lerp.
- `tick(list)`: runs every 16ms via Swing Timer in `BoardPanel`.
- `TokenAnimator.setImmediate(ids)` / `clearImmediate()`: bypasses lerp for locally-dragged tokens.
- `PointerAnimator` keys entries by `Triple(red, green, blue)` (the client's assigned color).

### Mouse interaction (`BoardMouseController`)
State machine modes: `IDLE`, `DRAG_TOKENS`, `RECT_SELECT`, `RECT_SELECT_ADDITIVE`, `RMB_ROTATE`, `MMB_PAN`.
- **LMB**: token hit-test → select + drag or rect-select. Ctrl adds to selection. Ctrl + no-move = toggle.
- **RMB** (selection non-empty): horizontal drag = rotate selected tokens (1px = 1°), sent via debouncer.
- **MMB**: drag pans viewport (`offsetX/Y += screenDelta / zoom`).
- **Scroll wheel**: zoom anchored at cursor. `Ctrl + scroll`: rotate selected ±10°, debouncer flushed immediately.
- **WASD / arrows**: pan viewport 50 screen-px per keypress (`WHEN_IN_FOCUSED_WINDOW`). Right/D = camera right = `offsetX -= step`.
- **Delete / Backspace**: `BoardActions.delete()`.

### Command debouncing (`CommandDebouncer`)
Swing Timer at 50ms. Pending map `TokenId → Adjustment` (last-write-wins per field). `flush()` called on mouse release, RMB release, and wheel rotation for immediate send.

### Pointer sending (`PointerCommandSender`)
Listens to mouse moves on `BoardPanel`. Converts screen coords to world via `ViewportState.screenToWorld`, sends `MovePointerCommand`. Lives in `view/action/`.

### Sprite tree panel (`SpriteBagListPanel`)
`JTree` backed by `DefaultMutableTreeNode`. Structure built from `SpriteBagDirectoryLoader.getFolderStructure()` (scans `sprites/` subdirs for PNG prefixes — no image loading). Folder nodes auto-expand on `refresh()`. Leaf double-click = spawn at null (random). Drag exports `SpriteBagId.id` string via `stringFlavor`.

### Sprite loading
`SpriteLoader` and `SpriteBagDirectoryLoader` are both in `client/application/`, package `net.rafkos.ojkipojki.client.application`.
