package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent

class UploadSpriteBagsCommandHandler : Handler<UploadSpriteBagsCommand> {
    override fun handle(action: UploadSpriteBagsCommand) {
        action.spriteBags.forEach { state ->
            val model = ServerContext.modelRepository.findSpriteBagById(state.id) ?: SpriteBagModel()
            model.apply(state)
            ServerContext.modelRepository.saveSpriteBag(model)
        }
        val state = ServerContext.modelRepository.findAllSpriteBags().map { it.toState() }.toList()
        ServerContext.eventBroadcastService.broadcast(SpriteBagsUpdatedEvent(state))
    }
}