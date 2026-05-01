package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.panel.BoardPanel
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.protocol.command.MovePointerCommand
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Timer

class PointerCommandSender(
    private val transmitter: CommandTransmitter,
    private val viewportState: ViewportState,
    private val boardPanel: BoardPanel,
) : MouseAdapter() {

    private var pendingX: Int? = null
    private var pendingY: Int? = null
    private val timer = Timer(60) { flush() }

    init { timer.start() }

    override fun mouseMoved(e: MouseEvent) = update(e)
    override fun mouseDragged(e: MouseEvent) = update(e)

    private fun update(e: MouseEvent) {
        val world = viewportState.screenToWorld(e.point, boardPanel.width, boardPanel.height)
        pendingX = world.x
        pendingY = world.y
    }

    private fun flush() {
        val x = pendingX ?: return
        val y = pendingY ?: return
        pendingX = null
        pendingY = null
        transmitter.transmit(MovePointerCommand(x, y))
    }

    fun stop() = timer.stop()
}
