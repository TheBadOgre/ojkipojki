package net.rafkos.ojkipojki.client.view.input

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.panel.BoardPanel
import net.rafkos.ojkipojki.client.view.render.TokenRenderer
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class BoardMouseController(
    private val boardPanel: BoardPanel,
    private val stateRepository: StateRepository,
    private val selectionState: SelectionState,
    private val viewportState: ViewportState,
    private val debouncer: CommandDebouncer,
    private val tokenRenderer: TokenRenderer,
) : MouseAdapter() {

    private enum class Mode { IDLE, DRAG_TOKENS, RECT_SELECT, RECT_SELECT_ADDITIVE, RMB_ROTATE, MMB_PAN }

    private var mode = Mode.IDLE
    private var pressScreenX = 0
    private var pressScreenY = 0
    private var ctrlHeld = false
    private var moved = false
    private var clickedTokenId: TokenId? = null
    private var initialPositions = mapOf<TokenId, Position>()

    override fun mousePressed(e: MouseEvent) {
        boardPanel.requestFocusInWindow()

        when (e.button) {
            MouseEvent.BUTTON2 -> {
                pressScreenX = e.x
                pressScreenY = e.y
                mode = Mode.MMB_PAN
                return
            }
            MouseEvent.BUTTON3 -> {
                if (selectionState.selectedIds().isNotEmpty()) {
                    pressScreenX = e.x
                    mode = Mode.RMB_ROTATE
                }
                return
            }
        }

        // LMB
        pressScreenX = e.x
        pressScreenY = e.y
        ctrlHeld = e.isControlDown
        moved = false

        val worldPos = viewportState.screenToWorld(e.point, boardPanel.width, boardPanel.height)
        val token = findTokenAt(worldPos.x.toDouble(), worldPos.y.toDouble())

        when {
            token != null && !selectionState.contains(token.id) && !ctrlHeld -> {
                selectionState.replaceWith(listOf(token.id))
                clickedTokenId = token.id
                captureInitialPositions()
                mode = Mode.DRAG_TOKENS
            }
            token != null && !selectionState.contains(token.id) && ctrlHeld -> {
                selectionState.addAll(listOf(token.id))
                clickedTokenId = token.id
                captureInitialPositions()
                mode = Mode.DRAG_TOKENS
            }
            token != null && selectionState.contains(token.id) -> {
                clickedTokenId = token.id
                captureInitialPositions()
                mode = Mode.DRAG_TOKENS
            }
            token == null && !ctrlHeld -> {
                selectionState.clear()
                boardPanel.dragRectOverlay = DragRectOverlay(e.x, e.y)
                mode = Mode.RECT_SELECT
            }
            token == null && ctrlHeld -> {
                boardPanel.dragRectOverlay = DragRectOverlay(e.x, e.y)
                mode = Mode.RECT_SELECT_ADDITIVE
            }
        }
        boardPanel.repaint()
    }

    override fun mouseDragged(e: MouseEvent) {
        moved = true
        when (mode) {
            Mode.DRAG_TOKENS -> {
                val screenDx = (e.x - pressScreenX).toDouble()
                val screenDy = (e.y - pressScreenY).toDouble()
                val (worldDx, worldDy) = viewportState.screenDeltaToWorld(screenDx, screenDy)
                selectionState.selectedIds().forEach { id ->
                    val initial = initialPositions[id] ?: return@forEach
                    val newPos = Position(initial.x + worldDx, initial.y + worldDy)
                    stateRepository.findTokenById(id)?.let { stateRepository.saveToken(it.copy(position = newPos)) }
                    debouncer.enqueue(MoveTokensCommand.TokenIdAndPosition(id, newPos, null, null, null))
                }
            }
            Mode.RECT_SELECT, Mode.RECT_SELECT_ADDITIVE -> {
                boardPanel.dragRectOverlay?.let { it.endX = e.x; it.endY = e.y }
            }
            Mode.RMB_ROTATE -> {
                val dx = e.x - pressScreenX
                pressScreenX = e.x
                val delta = dx.toDouble()
                selectionState.selectedIds().forEach { id ->
                    val token = stateRepository.findTokenById(id) ?: return@forEach
                    val newRot = Rotation((token.rotation.degrees + delta + 360.0) % 360.0)
                    stateRepository.saveToken(token.copy(rotation = newRot))
                    debouncer.enqueue(MoveTokensCommand.TokenIdAndPosition(id, null, newRot, null, null))
                }
            }
            Mode.MMB_PAN -> {
                viewportState.offsetX += (e.x - pressScreenX) / viewportState.zoom
                viewportState.offsetY += (e.y - pressScreenY) / viewportState.zoom
                pressScreenX = e.x
                pressScreenY = e.y
            }
            else -> {}
        }
        boardPanel.repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
        when (mode) {
            Mode.DRAG_TOKENS -> {
                debouncer.flush()
                if (!moved && ctrlHeld) clickedTokenId?.let { selectionState.toggle(it) }
            }
            Mode.RECT_SELECT, Mode.RECT_SELECT_ADDITIVE -> {
                val rect = boardPanel.dragRectOverlay?.toRectangle()
                boardPanel.dragRectOverlay = null
                if (rect != null && (rect.width > 2 || rect.height > 2)) {
                    val inRect = stateRepository.findAllTokens().filter { token ->
                        val sp = viewportState.worldToScreen(token.position, boardPanel.width, boardPanel.height)
                        rect.contains(sp)
                    }.map { it.id }
                    if (mode == Mode.RECT_SELECT_ADDITIVE) selectionState.addAll(inRect)
                    else selectionState.replaceWith(inRect)
                }
            }
            Mode.RMB_ROTATE -> debouncer.flush()
            else -> {}
        }
        mode = Mode.IDLE
        boardPanel.repaint()
    }

    private fun findTokenAt(worldX: Double, worldY: Double): Token? =
        stateRepository.findAllTokens()
            .sortedBy { it.index.value }
            .reversed()
            .firstOrNull { token ->
                val sprite = stateRepository.findSpriteById(token.spriteId) ?: return@firstOrNull false
                tokenRenderer.hitTest(token, sprite, worldX, worldY)
            }

    private fun captureInitialPositions() {
        initialPositions = selectionState.selectedIds().associateWith { id ->
            stateRepository.findTokenById(id)?.position ?: Position(0, 0)
        }
    }
}
