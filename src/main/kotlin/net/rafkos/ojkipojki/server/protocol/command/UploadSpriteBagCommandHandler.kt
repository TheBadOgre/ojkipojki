package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagCommand

class UploadSpriteBagCommandHandler(
    private val eventBroadcastService: EventBroadcastService
) : Handler<UploadSpriteBagCommand> {
    override fun handle(action: UploadSpriteBagCommand) {
        println("test")
    }
}