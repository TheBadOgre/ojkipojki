package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.LockTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent

class LockTokensCommandHandler : Handler<LockTokensCommand> {
    override fun handle(action: LockTokensCommand) {
        val updatedModels = action.tokenIds.mapNotNull { id ->
            val model = ServerContext.modelRepository.findTokenById(id) ?: return@mapNotNull null
            model.locked = action.locked
            ServerContext.modelRepository.saveToken(model)
            model
        }
        val tokenActions = updatedModels.map { SomeTokensUpdatedEvent.TokenAction.Update(it.toState()) }
        ServerContext.eventBroadcastService.broadcast(SomeTokensUpdatedEvent(tokenActions))
    }
}
