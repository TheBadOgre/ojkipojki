package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent

class MissingSpriteBagsEventHandler : Handler<MissingSpriteBagsEvent> {
    override fun handle(action: MissingSpriteBagsEvent) {
        val matching = ClientContext.localSpriteBagRegistry.bags()
            .filter { it.id in action.ids }
        if (matching.isEmpty()) return
        ClientContext.commandTransmitter?.transmit(UploadSpriteBagsCommand(matching))
    }
}
