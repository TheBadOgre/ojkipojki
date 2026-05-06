# ojkipojki

Minimal tabletop simulator. Load sprite sheets, spawn tokens, move them.

## Architecture

Client-server over TCP sockets. **Server owns all mutable state**; client holds read-only mirror updated via events.

```
Client --Command (TCP)--> Server (commandExecutor: single-threaded ExecutorService)
                              update ModelRepository
Client <--Event (TCP, broadcast)-- Server
update StateRepository
```

### Server (`server/`)
- `ModelRepository` — mutable models (`SpriteBagModel`, `SpriteModel`, `TokenModel`) in `ConcurrentHashMap`. Models have `apply(domain)` and `toState()`.
- `commandExecutor` (`ServerContext`) — single-threaded; serializes all command dispatch.
- `CommandContext` — `ThreadLocal<String?>` clientId, set per-task before `dispatch`.
- `EventBroadcastService` — `broadcast(event)` to all, `broadcast(event, clientId)` unicast, `broadcastCustom(excludeClientId, factory)` per-recipient (used for `PointersUpdatedEvent`).
- `PointerRepository`, `ClientColorRegistry` (32-color pool), `AutoSaveService` (5min daemon Timer → `last_game.sav`), `TokenSyncHeartbeatService` (5s full `TokensSyncEvent`).
- `GameLoader.tryLoad` returns false on corruption (repo cleared; caller surfaces error).

### Client (`client/`)
- `StateRepository` — immutable domain objects in `ConcurrentHashMap`.
- `ClientContext` — service locator + nullable callbacks. **`onTokensUpdated` fires every token change; `onTokensCountChanged` fires only on count change** (spawn/delete/sync) — used to gate expensive UI rebuilds.
- `ApplicationHandler` — wires callbacks to UI, uploads local sprites on connect.
- `SpriteLoader` / `SpriteBagDirectoryLoader` (in `client/application/`).

### Shared (`shared/`)
**Domain (immutable, Serializable, all have `serialVersionUID`):**
- `SpriteBag(id, groupName, sprites)`, `Sprite(id, frontImageBytes, backImageBytes)` (WebP)
- `Token(id, spriteId, position, rotation, index, flipped, locked)`
- `Pointer(x, y, red, green, blue)` — keyed by assigned RGB color
- IDs: `SpriteBagId(String)`, `SpriteId(SpriteBagId, r, g, b)`, `TokenId(UUID)`

**Protocol:** `Handler<A>`, `Dispatcher<A>`, `Receiver`, `Transmitter`.

## Commands and Events

| Direction | Type | Notes |
|---|---|---|
| C→S | `UploadSpriteBagsCommand` | Upsert, broadcast `SpriteBagsUpdatedEvent` |
| C→S | `SpawnTokensCommand(bagId, position?, spriteId?)` | One token per sprite (or single if `spriteId`) |
| C→S | `MoveTokensCommand(adjustments)` | null field = no change |
| C→S | `DeleteTokensCommand`, `ShuffleTokensCommand`, `LockTokensCommand` | |
| C→S | `MovePointerCommand` | `broadcastCustom` excludes sender |
| S→C | `SomeTokensUpdatedEvent` | Per-token Update/Delete delta |
| S→C | `TokensSyncEvent` | Full list — replaces all. Sent on connect + 5s heartbeat |
| S→C | `SpriteBagsUpdatedEvent`, `PointersUpdatedEvent`, `ConnectedClientsUpdateEvent` | |

## Sprite loading

`SpriteLoader` reads `{id}_front.png`, `{id}_back.png`, `{id}_mask.png`. Each non-black RGB in mask = one sprite (becomes `SpriteId.r/g/b`). Loader bounding-boxes each color region, crops front/back, masks other sprites transparent.

## UI architecture (`client/view/`)

Swing. State in `state/`, render in `render/`, input in `input/`. **No logic in panels.**

### Threading
- All Swing on EDT. Network events → `SwingUtilities.invokeLater`.
- `StateRepository` thread-safe. `TokenAnimator`, `PointerAnimator`, `SelectionState`, `ViewportState` are EDT-only.

### Coordinate system (`ViewportState`)
`screen = (world + offset) * zoom + panelCenter`. `affineTransform` = `translate(panelCenter) * scale(zoom) * translate(offset)`, applied with `g2.transform(at)` to preserve HiDPI base transform.

### Rendering order
1. Grid in world space.
2. Tokens sorted by `index` ascending (lowest = bottom). Each: `tokenAnimator.visualize(token)` → `TokenRenderer.draw()`.
3. Pointers via `pointerAnimator.visualize`.
4. Drag-rect overlay in screen space.

Hit-testing reverses render order (topmost first). Selection outline 1px `#50A0FF` dashed, width `1/zoom`.

### `TokenRenderer` depth effect
Three cached layers per `SpriteId`: shadow `(+5,+5)` 45% black alpha-mask; dark edge `(+2,+2)` brightness 35%; main image. Shadow/edge from front only (not per-flipped).

### Animators (`TokenAnimator`, `PointerAnimator`)
Lerp factor 0.25, tick 16ms via Swing Timer in `BoardPanel`. New entries init at target (no jump).

**`TokenAnimator` drag override (critical):** `setImmediate(ids)` + `setDragPosition/Rotation` write to `dragOverride` map. `visualize` reads from `dragOverride` for immediate tokens instead of `stateRepository`, so server echoes (~50ms-old positions) cannot cause visual jumps mid-drag. `tick` snaps `states` to `dragOverride` so lerp on release starts from correct position.

`PointerAnimator` keys by `Triple(r,g,b)`.

### Mouse (`BoardMouseController`)
Modes: `IDLE`, `DRAG_TOKENS`, `RECT_SELECT`, `RECT_SELECT_ADDITIVE`, `RMB_ROTATE`, `MMB_PAN`. RMB rotate = 1px/°. Ctrl+scroll = ±10° flushed immediately. WASD/arrows pan 50 screen-px. Right/D = camera-right = `offsetX -= step`.

### Debouncing (`CommandDebouncer`)
Swing Timer 50ms. `TokenId → Adjustment` last-write-wins per field. `flush()` on mouse/RMB release and wheel.

### Sprite tree (`SpriteBagListPanel`)
`JTree`. Structure from `SpriteBagDirectoryLoader.getFolderStructure()` — scans dirs only, no image load. Leaf double-click = spawn at null. Drag exports `SpriteBagId.id` via `stringFlavor`.

## CLI (`CliRunner`)

`--server` flag in `main()` routes to `CliRunner.run(args)`.

`CliRunner.parseArgs(args): ParseResult` — testable, no side-effects. Returns `Success(port, saveFile?)` or `Error(message)`.

| Flag | Effect |
|---|---|
| _(none)_ | loads `autosave.sav` from `savesDir` |
| `--start-fresh` | `saveFile = null` (empty repo) |
| `--load-save <filename>` | resolves `File(savesDir, filename)` |
| `--scenario <filename>` | resolves `File(scenariosDir, filename)` |
| `--port <int>` | server port (default 12001) |

`--load-save`, `--start-fresh`, `--scenario` are mutually exclusive — any two together → `ParseResult.Error`.

## Scenarios

Save-format files (`.sav`) in `scenariosRoot`. Same GZIP/ObjectStream format as saves. Users copy any save → scenarios dir to snapshot a board state.

- `GamePersistence.listScenarioFiles()` — returns `.sav` files sorted alphabetically (no timestamp relevance).
- Launcher: scenarios appended after saves in the list, displayed as `name [scenario]` (no date).
- CLI: `--scenario <filename>` alternative to `--load-save`.
- Scenarios are bundled with the app (staged via `--app-content`); saves are user-generated.

## Save format

`AppDirs.resolveData("saves")/`. GZIP-wrapped Java `ObjectOutputStream` containing a `GameSave`.

### Versioning
- `GameSave` marker interface, `GameDataSerializer<G>`, `GameDataDeserializer<G>`, common `GameData` result.
- `GamePersistence` writes via latest serializer, reads with `when (is GameDataVN)`.
- New version: implement `GameDataV2` + serializer + deserializer, add `when` branch, update `GamePersistence.serializer`.

### V1 — tokens only, NOT sprite bags
Sprite WebP bytes already compressed (GZIP useless), and clients re-upload on connect via `UploadSpriteBagsCommand`. Saves are KB-tiny. `GameData.spriteBags = emptyList()` after load — `GameLoader` tolerates.

`SpriteId` encoded `"${bagId}:${r}:${g}:${b}"`. Decode splits from right (last 3 segments = rgb), so colons in bag names safe.

### Init dialog / sprite-less restart
Server with no sprites: `ClientSessionManager.onClientConnected` sends `SpriteBagsUpdatedEvent(emptyList)` then `GameInitializationEvent(DONE)`. UI locked by init dialog. Client immediately sends `UploadSpriteBagsCommand`; handler emits `IN_PROGRESS` (re-locks) then `DONE` (unlocks). User never sees broken-sprite state.

## Conventions

- **Logging:** log4j via `LogManager.getLogger(Cls::class.java)`, prefer companion val.
- **Locale:** all UI text via `LocaleService.get(key, ...args)`. Loaded once at startup; en + system-language overlay. Console logs not localized.
- **Tests required:** new command/event/handler **must** have a serialization test + handler test, and be registered in dispatcher (covered by dispatcher coverage tests). Enforced by `./gradlew test` which runs before `release_all`.
- **`AppDirs`** resolves paths via `app.dir` system property (set by jpackage launcher from `$APPDIR`); CWD fallback for local dev.
  - `spritesRoot`: one level above `$APPDIR` packaged, `./sprites/` local.
  - `scenariosRoot`: one level above `$APPDIR` packaged, `./scenarios/` local. Staged via `--app-content` same as sprites.
  - `dataRoot` (`resolveData(path)`): user-writable. Linux `~/.local/share/ojkipojki/`, macOS `~/Library/Application Support/ojkipojki/`, Windows one level above `$APPDIR`, local-dev CWD.

## Tests

`./gradlew test`. Mirrors main package structure under `src/test/kotlin`.

### Fixtures
- `ServerContextFixture` — fresh real repos + mocked `EventBroadcastService`/`CommandDispatcher` per test.
- `ClientContextFixture` — fresh `StateRepository` + `EventDispatcher`, nulled callbacks.
- `socketPair()` — connected loopback `(client, accepted)`. **Create peer's `ObjectOutputStream` BEFORE the other side's `ObjectInputStream` constructor**, else it blocks.
- `await()` — poll-with-timeout, use instead of `Thread.sleep`.

### Gotchas
- `ServerContext`/`ClientContext` are JVM-wide singletons. Reinstall fixture in `@BeforeEach`; reset `CommandContext.clientId = null` in `@AfterEach` for handler tests.
- `EventBroadcastService` mocked. `broadcast(event)` and `broadcast(event, clientId)` are separate overloads.
- `broadcastCustom` first arg `excludeClientId: String?` — use `anyOrNull()` not `any()` if call may pass null.
- Sequential test execution required (singletons not isolated).

## Build & run

```
./gradlew run          # copies local_resources/ → last_run_tmp/, runs there
./gradlew release_all  # clean → build → jpackage → output/
```

`./last_run_tmp/` persists between runs (holds saves). `./sprites/` and `./scenarios/` subdirs must exist there (copied from `local_resources/`).

**jpackage is OS-native — runs only build host OS.** Artifacts: Windows zip, Linux deb+rpm, macOS dmg.

### Packaged layout

| Platform | `$APPDIR` | Sprites | Scenarios | Saves |
|---|---|---|---|---|
| Windows zip | `<app>/app/` | `<app>/sprites/` | `<app>/scenarios/` | `<app>/saves/` |
| Linux deb | `/opt/ojkipojki/lib/app/` | `/opt/ojkipojki/sprites/` | `/opt/ojkipojki/scenarios/` | `~/.local/share/ojkipojki/saves/` |
| macOS dmg | `<App>.app/Contents/app/` | `<App>.app/Contents/sprites/` | `<App>.app/Contents/scenarios/` | `~/Library/Application Support/ojkipojki/saves/` |
| Local dev | CWD `last_run_tmp/` | `last_run_tmp/sprites/` | `last_run_tmp/scenarios/` | `last_run_tmp/saves/` |

`sprites/` and `scenarios/` staged into `jpackageContentDir` via `--app-content` (land one level above `$APPDIR`). `saves/` never bundled — created on first save (`savesDir.mkdirs()` in `GamePersistence.save`).
