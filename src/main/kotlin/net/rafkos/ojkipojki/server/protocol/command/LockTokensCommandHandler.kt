package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.LockTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class LockTokensCommandHandler : Handler<LockTokensCommand> {
    override fun handle(action: LockTokensCommand) {
        action.tokenIds.forEach { id ->
            val model = ServerContext.modelRepository.findTokenById(id) ?: return@forEach
            model.locked = action.locked
            ServerContext.modelRepository.saveToken(model)
        }
        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }
}
