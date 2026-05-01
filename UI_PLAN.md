# UI Implementation Plan — `client/view`

Swing-based UI for ojkipojki. Renders tokens, sprite bag list, toolbar actions, status bar. All client state and rendering separated from network protocol.

## 1. Protocol additions (shared)

**New command** — `shared/protocol/command/DeleteTokensCommand.kt`
```kotlin
data class DeleteTokensCommand(val tokenIds: List<TokenId>) : Command
```

**Modify** — `shared/protocol/command/SpawnTokensCommand.kt`
- Add `position: Position? = null`. Null → current random behavior. Non-null → tokens stack at given world position (`x = base.x`, `y = base.y + i*20`).

**ModelRepository** — add `deleteToken(id: TokenId)` and `deleteAllTokens()`.

**Server handlers**:
- `DeleteTokensCommandHandler` — remove tokens, broadcast `TokensUpdatedEvent`.
- Update `SpawnTokensCommandHandler` for optional position.
- Register `DeleteTokensCommand` in `CommandDispatcher`.

## 2. Package layout — `client/view/`

```
client/view/
├── MainWindow.kt                 # JFrame entry, BorderLayout wiring
├── panel/
│   ├── ToolbarPanel.kt           # NORTH — JToolBar with action buttons
│   ├── SpriteBagListPanel.kt     # EAST — JList of bags + drag source
│   ├── BoardPanel.kt             # CENTER — custom paintComponent
│   └── StatusBarPanel.kt         # SOUTH — connection IP label
├── render/
│   └── TokenRenderer.kt          # draws single token (transform, image, selection halo)
├── input/
│   ├── BoardMouseController.kt   # mouse listener for board (select/drag/rect)
│   ├── BoardWheelController.kt   # zoom + ctrl-wheel rotation
│   └── DragRectOverlay.kt        # transient rect-select shape state
├── state/
│   ├── SelectionState.kt         # Set<TokenId>, observable, prunes dangling ids
│   └── ViewportState.kt          # zoom + pan offset, world↔screen transforms
├── action/
│   ├── BoardActions.kt           # selectAll, deselect, rotate60, indexUp/Down, delete, refreshBags
│   ├── CommandDebouncer.kt       # coalesces drag/rotate → batched MoveTokensCommand
│   └── SpriteBagSpawnHandler.kt  # double-click + DnD drop → SpawnTokensCommand
└── loader/
    └── SpriteBagDirectoryLoader.kt # scans sprites/<sub>/, calls SpriteLoader, returns List<SpriteBag>
```

Swing classes hold no logic — all business state in `state/`, transforms in `ViewportState`, debouncing in `CommandDebouncer`. Panels observe state and repaint.

## 3. Coordinate system

`ViewportState`:
- `zoom: Double` (default 1.0)
- `offsetX, offsetY: Double` (pan, world units)
- `worldToScreen(p: Position): Point2D` → `((p.x + offsetX) * zoom + panelW/2, (p.y + offsetY) * zoom + panelH/2)`
- `screenToWorld(p: Point2D): Position` → inverse
- `screenDeltaToWorld(dx, dy): Pair<Int,Int>` → for drag deltas

World coords stay `Int` (matches `Position`). Zoom only affects rendering.

## 4. Selection — `SelectionState`

- `selectedIds: MutableSet<TokenId>` + listeners
- `select(id)`, `toggle(id)`, `replaceWith(ids)`, `clear()`, `addAll(ids)`
- `pruneAgainst(currentTokens)` called on every `TokensUpdatedEvent` — drops ids no longer present
- Wire from `TokensUpdatedEventHandler`: after `replaceAllTokens`, call `selectionState.pruneAgainst(...)` and trigger board repaint

Same pruning hook on `SpriteBagsUpdatedEvent` triggers `SpriteBagListPanel` refresh.

## 5. BoardPanel

`paintComponent`:
1. Fill background.
2. `g.transform = viewportState.affineTransform()` — apply zoom + offset.
3. Sort tokens by `index.value` ascending, render each via `TokenRenderer` (sprite image, position, rotation, flipped).
4. For selected tokens, paint red outline (post-transform or stroke-width-corrected for zoom).
5. If drag-rect active, paint translucent rectangle.

`TokenRenderer.draw(g, token, sprite, selected)`:
- `AffineTransform`: translate to position → rotate around token center → draw image (front or back per `flipped`).
- If selected, stroke red rect around bounds.

Sprite images cached: decode `Sprite.frontImageBytes` / `backImageBytes` to `BufferedImage` once per `SpriteId`, store in `Map<SpriteId, Pair<BufferedImage,BufferedImage>>` inside renderer.

## 6. Mouse interaction (BoardPanel)

`BoardMouseController` modes (state machine):

**Idle → Press**:
- Hit-test: token under cursor?
  - **Yes, unselected, no ctrl**: replace selection with this token. Enter `DRAG_TOKENS`.
  - **Yes, unselected, ctrl**: add to selection. Enter `DRAG_TOKENS`.
  - **Yes, already selected**: enter `DRAG_TOKENS`. (ctrl on release toggles off — see below.)
  - **No (background), no ctrl**: clear selection. Enter `RECT_SELECT`.
  - **No, ctrl**: enter `RECT_SELECT_ADDITIVE`.

**DRAG_TOKENS (drag)**:
- Compute world delta from press point.
- Apply delta to in-memory selected token positions (visual preview).
- Enqueue position updates via `CommandDebouncer`.

**DRAG_TOKENS (release)**:
- Flush debouncer immediately.
- If no movement and ctrl was held, toggle this token off selection.

**RECT_SELECT (drag)**:
- Update `DragRectOverlay`, repaint.

**RECT_SELECT (release)**:
- Find tokens whose center is in rect.
- Replace selection (or add if additive variant).

## 7. Zoom + rotate (BoardWheelController)

- `wheel, no ctrl` → adjust `viewportState.zoom` (clamp 0.1–8.0, multiplicative step 1.1). Anchor at cursor (world point under cursor stays put — adjust offset accordingly).
- `wheel + ctrl + has selection` → rotate each selected token by `±10°` (sign from wheel direction). Apply to in-memory rotation, push via debouncer.
- `wheel + ctrl + no selection` → no-op.

## 8. CommandDebouncer

```
class CommandDebouncer(transmitter: CommandTransmitter, intervalMs: Long = 50)
```

- Pending map `MutableMap<TokenId, MoveTokensCommand.Adjustment>` — last write wins per token per field.
- `swing.Timer(intervalMs)` repeating; on tick if pending non-empty → build `MoveTokensCommand`, send, clear.
- `flush()` called on mouse-release / wheel idle to send immediately.
- Threading: all calls on EDT (Swing Timer fires on EDT — safe).

## 9. Toolbar actions

`ToolbarPanel` has 8 `JButton`s, icons set later via setter. Each button calls into `BoardActions`:

| Button | Action |
|---|---|
| Select all | `SelectionState.replaceWith(allTokenIds)` |
| Deselect all | `SelectionState.clear()` |
| Deselect selected | same as deselect all (alias — confirm intent later) |
| Rotate +60° | per selected: rotation += 60, send `MoveTokensCommand` immediately |
| Index − | per selected: index -= 1, send |
| Index + | per selected: index += 1, send |
| Delete | send `DeleteTokensCommand(selectedIds)`, clear selection |
| Refresh bags | `SpriteBagDirectoryLoader.loadAll()` → send `UploadSpriteBagsCommand` |

## 10. SpriteBagListPanel

- `JList<SpriteBag>` with custom cell renderer (just bag id text for now).
- Backed by `DefaultListModel<SpriteBag>` populated from `StateRepository.findAllSpriteBags()` whenever `SpriteBagsUpdatedEvent` fires.
- **Double-click** → `SpriteBagSpawnHandler.spawn(bag, position = null)`.
- **Drag source** via `TransferHandler` exporting `SpriteBagId` as string `DataFlavor`.

## 11. BoardPanel as drop target

- Custom `TransferHandler.canImport` accepts the bag-id flavor.
- On drop, `support.dropLocation.dropPoint` → `viewportState.screenToWorld(...)` → send `SpawnTokensCommand(bagId, position)`.

## 12. SpriteBagDirectoryLoader

- `loadAll(rootDir: Path = "sprites"): List<SpriteBag>`
- For each subdirectory of `sprites/`, call existing `SpriteLoader.load(subdir)`.
- Aggregates results.
- Toolbar "Update sprite bags" button + initial load on `MainWindow` start both call this then send `UploadSpriteBagsCommand`.

## 13. StatusBarPanel

- `JLabel("Connected to: $serverIp")`. IP passed in constructor from `ClientRunner`.

## 14. Wiring through ClientRunner

After `ClientRunner` connects:
1. Build `SelectionState`, `ViewportState`, `CommandDebouncer`.
2. Build `MainWindow(serverIp, clientContext, selectionState, viewportState, debouncer)`.
3. Hook `TokensUpdatedEventHandler` and `SpriteBagsUpdatedEventHandler` to also trigger view repaint + selection prune + bag list refresh (via shared listener registry on `StateRepository`, or direct callbacks injected through `ClientContext`).
4. `mainWindow.isVisible = true` on EDT (`SwingUtilities.invokeLater`).
5. Initial sprite bag load: call `SpriteBagDirectoryLoader.loadAll()` → `UploadSpriteBagsCommand`.

## 15. Threading

- All Swing work on EDT.
- Network events arrive on socket thread → handlers dispatch repaint/list-refresh via `SwingUtilities.invokeLater`.
- `StateRepository` already thread-safe (`ConcurrentHashMap`), reads on EDT are fine.
