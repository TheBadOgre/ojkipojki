# ojkipojki

Simplified tabletop simulator. Lets you load sprite sheets, spawn tokens on a board, and move them. Think Tabletop Simulator but minimal.

## Current state

`Main.kt` opens `LauncherWindow`. From there, user connects to a server or hosts one.

## Architecture

Client-server over TCP sockets. Server owns all mutable state. Client holds a read-only mirror of that state updated via events.

```
Client                          Server
  |                               |
  |-- Command (TCP) ------------> |
  |                         handle command
  |                         update ModelRepository
  |                               |
  | <-- Event (TCP, broadcast) -- |
  update StateRepository          |
```

### Server (`server/`)

- **`ModelRepository`** — in-memory store (`ConcurrentHashMap`) for mutable model objects: `SpriteBagModel`, `SpriteModel`, `TokenModel`.
- **`CommandDispatcher`** — routes incoming commands to their handler by type.
- **`EventBroadcastService`** — sends an event to all connected clients.
- **`ClientSessionManager`** / **`ConnectionManager`** — manage TCP connections and per-client sessions.

Models (`server/model/`) are mutable classes with `apply(domain)` to absorb domain state and `toState()` to produce domain objects for events.

### Client (`client/`)

- **`StateRepository`** — in-memory store (`ConcurrentHashMap`) for immutable domain objects: `SpriteBag`, `Sprite`, `Token`.
- **`EventDispatcher`** — routes incoming events to their handler by type.
- **`CommandTransmitter`** — sends commands to the server over the socket.
- **`SpriteLoader`** — loads sprite bags from a directory of PNGs (see below).

UI is not yet implemented.

### Shared (`shared/`)

Domain classes and protocol interfaces used by both sides.

**Domain objects** (immutable, used as-is on client):
- `SpriteBag(id, sprites)` — a named collection of sprites.
- `Sprite(id, frontImageBytes, backImageBytes)` — one game piece, front and back images as PNG bytes.
- `Token(id, spriteId, position, rotation, index, flipped)` — a placed instance of a sprite on the board.

**IDs:**
- `SpriteBagId(String)` — simple string name (e.g. `"dice"`).
- `SpriteId(SpriteBagId, red, green, blue)` — composite key; the RGB comes from the mask file (see SpriteLoader).
- `TokenId(UUID)` — random UUID assigned at spawn.

**Protocol abstractions:** `Handler<A>`, `Dispatcher<A>`, `Receiver`, `Transmitter` — generic interfaces for the command/event pipeline.

## Commands and Events

| Direction | Type | Effect |
|---|---|---|
| Client → Server | `UploadSpriteBagsCommand(spriteBags)` | Upsert sprite bags in `ModelRepository`, broadcast `SpriteBagsUpdatedEvent` |
| Client → Server | `SpawnTokensCommand(spriteBagId)` | Create one `TokenModel` per sprite in the bag (random x ∈ [-100,100], y increments by 20), broadcast `TokensUpdatedEvent` |
| Client → Server | `MoveTokensCommand(adjustments)` | Update position/rotation/index/flipped on existing tokens; null field = no change; broadcast `TokensUpdatedEvent` |
| Server → Clients | `SpriteBagsUpdatedEvent(spriteBags)` | Full list of all sprite bags — client saves each, no purge |
| Server → Clients | `TokensUpdatedEvent(tokens)` | Complete token list — client replaces all tokens, no leftovers |

Events always carry **complete state**, never deltas.

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
│   └── ServerConsoleWindow.kt # Dark log window shown when hosting
├── shared/
│   ├── domain/          # Domain classes + IDs (immutable, Serializable)
│   └── protocol/        # Handler, Dispatcher, Receiver, Transmitter + command/event types
├── server/
│   ├── model/           # Mutable model classes (apply/toState)
│   ├── application/     # ModelRepository
│   └── protocol/
│       ├── command/     # CommandDispatcher + handlers
│       └── event/       # EventBroadcastService, EventTransmitter
└── client/
    ├── application/     # StateRepository, SpriteLoader
    ├── protocol/
    │   ├── command/     # CommandTransmitter (package: client.command)
    │   └── event/       # EventDispatcher + handlers
    └── view/
        ├── MainWindow.kt
        ├── state/       # SelectionState, ViewportState, TokenAnimator
        ├── action/      # BoardActions, CommandDebouncer, SpriteBagSpawnHandler
        ├── render/      # TokenRenderer
        ├── input/       # BoardMouseController, BoardWheelController, DragRectOverlay
        ├── panel/       # BoardPanel, ToolbarPanel, SpriteBagListPanel, StatusBarPanel
        └── loader/      # SpriteBagDirectoryLoader
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
- `TokenAnimator`, `SelectionState`, `ViewportState` are EDT-only — no sync needed.

### Coordinate system (`ViewportState`)
- `screen = (world + offset) * zoom + panelCenter`
- `worldToScreen` / `screenToWorld` for hit-testing and DnD drops.
- `affineTransform(w, h)`: `translate(panelCenter) * scale(zoom) * translate(offset)` — applied via `g2.transform(at)` (concatenates, preserves HiDPI base transform).

### Rendering (`BoardPanel.paintComponent`)
1. Apply viewport transform → draw grid in world space.
2. Sort tokens by `index.value` ascending (lowest = bottom).
3. Each token: `tokenAnimator.visualize(token)` for smooth position, then `TokenRenderer.draw()`.
4. Restore transform → draw drag-rect overlay in screen space.

### Token depth effect (`TokenRenderer`)
Three layers per token, drawn in world space under the token's local transform:
1. Shadow at `(+5, +5)`: alpha-mask of image, pure black, 45% opacity.
2. Dark edge at `(+2, +2)`: image darkened to 35% brightness — simulates thickness.
3. Main image at `(0, 0)`.
All three images cached per `SpriteId`. Shadow/edge computed once from front image (not per flipped state).

### Selection (`SelectionState`)
- `Set<TokenId>` + listener list. All mutation methods call `notifyListeners()`.
- `pruneAgainst(tokens)` called on every `TokensUpdatedEvent` to drop deleted token IDs.
- Selection outline: 1px blue (#50A0FF) dashed stroke, width corrected to `1/zoom` to stay 1px on screen.

### Hit testing (`BoardMouseController.findTokenAt`)
Sort tokens `sortedBy { index }.reversed()` — same order as rendering but reversed, so topmost visible token is checked first.

### Smooth animation (`TokenAnimator`)
- Holds `VisualState(x, y, rotation)` per token as floating-point.
- `syncWithTokens(tokens)`: called on `TokensUpdatedEvent` (EDT). New tokens initialise at target (no jump); existing tokens keep current visual state and lerp.
- `tick(tokens)`: runs every 16ms via Swing Timer in `BoardPanel`. Lerp factor 0.25 → ~150ms to reach 92% of distance.
- `setImmediate(ids)` / `clearImmediate()`: called on drag start/end to bypass animation for locally-dragged tokens (they must track cursor, not lerp).

### Mouse interaction (`BoardMouseController`)
State machine modes: `IDLE`, `DRAG_TOKENS`, `RECT_SELECT`, `RECT_SELECT_ADDITIVE`, `RMB_ROTATE`, `MMB_PAN`.
- **LMB**: token hit-test → select + drag or rect-select. Ctrl adds to selection. Ctrl + no-move = toggle.
- **RMB** (selection non-empty): horizontal drag = rotate selected tokens (1px = 1°), sent via debouncer.
- **MMB**: drag pans viewport (`offsetX/Y += screenDelta / zoom`).
- **Scroll wheel**: zoom anchored at cursor. `Ctrl + scroll`: rotate selected ±10°.
- **WASD / arrows**: pan viewport 50 screen-px per keypress (`WHEN_IN_FOCUSED_WINDOW`). Right/D = camera right = `offsetX -= step`.
- **Delete / Backspace**: `BoardActions.delete()`.

### Command debouncing (`CommandDebouncer`)
Swing Timer at 50ms. Pending map `TokenId → Adjustment` (last-write-wins per field). `flush()` called on mouse release and RMB release for immediate send.

### Sprite tree panel (`SpriteBagListPanel`)
`JTree` backed by `DefaultMutableTreeNode`. Structure built from `SpriteBagDirectoryLoader.getFolderStructure()` (scans `sprites/` subdirs for PNG prefixes — no image loading). Folder nodes auto-expand on `refresh()`. Leaf double-click = spawn at null (random). Drag exports `SpriteBagId.id` string via `stringFlavor`.

### Sprite loading
`SpriteLoader.loadSprites(dir)` is in package `client.application` but file lives in `client/util/` — import as `net.rafkos.ojkipojki.client.application.SpriteLoader`.
`CommandTransmitter` is in package `client.command` but file lives in `client/protocol/command/`.
