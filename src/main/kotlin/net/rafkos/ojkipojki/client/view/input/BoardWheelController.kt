package net.rafkos.ojkipojki.client.view.input

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.view.action.CommandDebouncer
import net.rafkos.ojkipojki.client.view.panel.BoardPanel
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

class BoardWheelController(
    private val boardPanel: BoardPanel,
    private val viewportState: ViewportState,
    private val selectionState: SelectionState,
    private val stateRepository: StateRepository,
    private val debouncer: CommandDebouncer,
) : MouseWheelListener {

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        if (e.isControlDown) {
            if (selectionState.selectedIds().isEmpty()) return
            val delta = if (e.wheelRotation < 0) 10.0 else -10.0
            selectionState.selectedIds().forEach { id ->
                val token = stateRepository.findTokenById(id) ?: return@forEach
                val newRot = Rotation((token.rotation.degrees + delta + 360.0) % 360.0)
                debouncer.enqueue(MoveTokensCommand.TokenIdAndPosition(id, null, newRot, null, null))
            }
        } else {
            val factor = if (e.wheelRotation < 0) 1.1 else 1.0 / 1.1
            val oldZoom = viewportState.zoom
            val newZoom = (oldZoom * factor).coerceIn(0.1, 8.0)
            val cx = e.x.toDouble()
            val cy = e.y.toDouble()
            val panelW = boardPanel.width.toDouble()
            val panelH = boardPanel.height.toDouble()
            viewportState.offsetX += (cx - panelW / 2.0) * (1.0 / newZoom - 1.0 / oldZoom)
            viewportState.offsetY += (cy - panelH / 2.0) * (1.0 / newZoom - 1.0 / oldZoom)
            viewportState.zoom = newZoom
            boardPanel.repaint()
        }
    }
}
