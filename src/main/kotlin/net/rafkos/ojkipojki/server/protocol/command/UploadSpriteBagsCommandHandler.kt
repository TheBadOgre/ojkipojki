package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent.Status
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.TokensSyncEvent

class UploadSpriteBagsCommandHandler : Handler<UploadSpriteBagsCommand> {
    override fun handle(action: UploadSpriteBagsCommand) {
        val oldSpriteIds = action.spriteBags.flatMap { bag ->
            ServerContext.modelRepository.findSpriteBagById(bag.id)?.sprites?.map { it.id } ?: emptyList()
        }.toSet()

        action.spriteBags.forEach { state ->
            val model = ServerContext.modelRepository.findSpriteBagById(state.id) ?: SpriteBagModel()
            model.apply(state)
            ServerContext.modelRepository.saveSpriteBag(model)
        }

        val newSpriteIds = action.spriteBags.flatMap { bag ->
            ServerContext.modelRepository.findSpriteBagById(bag.id)?.sprites?.map { it.id } ?: emptyList()
        }.toSet()

        val removedSpriteIds = oldSpriteIds - newSpriteIds
        if (removedSpriteIds.isNotEmpty()) {
            ServerContext.modelRepository.findAllTokens()
                .filter { it.spriteId in removedSpriteIds }
                .forEach { ServerContext.modelRepository.deleteToken(it.id) }
        }

        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensSyncEvent(tokens))

        val spriteBags = ServerContext.modelRepository.findAllSpriteBags().map { it.toState() }.toList()

        ServerContext.eventBroadcastService.broadcast(GameInitializationEvent(Status.IN_PROGRESS, "initialization.receivingSprites", 0.1))
        ServerContext.eventBroadcastService.broadcast(SpriteBagsUpdatedEvent(spriteBags))
        ServerContext.eventBroadcastService.broadcast(GameInitializationEvent(Status.DONE, "initialization.finished", 1.0))
    }
}