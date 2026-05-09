package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.domain.TokenId
import javax.swing.Timer

class CommandDebouncer(private val transmitter: CommandTransmitter, intervalMs: Long = 20) {
    private val pending = mutableMapOf<TokenId, MoveTokensCommand.TokenIdAndPosition>()
    private val timer = Timer(intervalMs.toInt()) { flush() }

    init { timer.start() }

    fun enqueue(adj: MoveTokensCommand.TokenIdAndPosition) {
        val ex = pending[adj.tokenId]
        pending[adj.tokenId] = adj.copy(
            position = adj.position ?: ex?.position,
            rotation = adj.rotation ?: ex?.rotation,
            flipped  = adj.flipped  ?: ex?.flipped,
            index    = adj.index    ?: ex?.index,
        )
    }

    fun flush() {
        if (pending.isEmpty()) return
        val adjustments = pending.values.toList()
        pending.clear()
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun stop() = timer.stop()
}
