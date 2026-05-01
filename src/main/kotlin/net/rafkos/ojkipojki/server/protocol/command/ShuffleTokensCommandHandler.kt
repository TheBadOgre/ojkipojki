package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.ShuffleTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class ShuffleTokensCommandHandler : Handler<ShuffleTokensCommand> {
    override fun handle(action: ShuffleTokensCommand) {
        val models = action.tokenIds
            .mapNotNull { ServerContext.modelRepository.findTokenById(it) }
            .filter { !it.locked }
        if (models.size < 2) return

        // Collect (position, index) slots from the selected tokens, shuffle them,
        // then reassign to the same models in original order.
        val slots = models.map { it.position.toState() to it.index.toState() }.shuffled()

        models.forEachIndexed { i, model ->
            model.position.apply(slots[i].first)
            model.index.apply(slots[i].second)
            ServerContext.modelRepository.saveToken(model)
        }

        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }
}
