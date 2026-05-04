package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent

class DeleteTokensCommandHandler : Handler<DeleteTokensCommand> {
    override fun handle(action: DeleteTokensCommand) {
        val deletedIds = action.tokenIds
            .filter { ServerContext.modelRepository.findTokenById(it)?.locked == false }
            .onEach { ServerContext.modelRepository.deleteToken(it) }
        val tokenActions = deletedIds.map { SomeTokensUpdatedEvent.TokenAction.Delete(it) }
        ServerContext.eventBroadcastService.broadcast(SomeTokensUpdatedEvent(tokenActions))
    }
}
