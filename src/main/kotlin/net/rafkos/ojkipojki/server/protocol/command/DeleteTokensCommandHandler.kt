package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class DeleteTokensCommandHandler : Handler<DeleteTokensCommand> {
    override fun handle(action: DeleteTokensCommand) {
        action.tokenIds.forEach { ServerContext.modelRepository.deleteToken(it) }
        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }
}
