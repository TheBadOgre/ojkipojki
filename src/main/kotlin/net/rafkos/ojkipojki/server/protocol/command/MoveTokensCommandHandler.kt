package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class MoveTokensCommandHandler : Handler<MoveTokensCommand> {
    override fun handle(action: MoveTokensCommand) {
        action.adjustments.forEach { adjustment ->
            val model = ServerContext.modelRepository.findTokenById(adjustment.tokenId) ?: return@forEach
            adjustment.position?.let { model.position.apply(it) }
            adjustment.rotation?.let { model.rotation.apply(it) }
            adjustment.index?.let { model.index.apply(it) }
            adjustment.flipped?.let { model.flipped = it }
            ServerContext.modelRepository.saveToken(model)
        }
        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }
}
