package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class MoveTokensCommandHandler : Handler<MoveTokensCommand> {
    override fun handle(action: MoveTokensCommand) {
        val oldIndices = mutableMapOf<TokenId, Int>()

        action.adjustments.forEach { adjustment ->
            val model = ServerContext.modelRepository.findTokenById(adjustment.tokenId) ?: return@forEach
            if (adjustment.index != null) oldIndices[adjustment.tokenId] = model.index.value
            adjustment.position?.let { model.position.apply(it) }
            adjustment.rotation?.let { model.rotation.apply(it) }
            adjustment.index?.let { model.index.apply(it) }
            adjustment.flipped?.let { model.flipped = it }
            ServerContext.modelRepository.saveToken(model)
        }
        if (oldIndices.isNotEmpty()) normalizeIndices(oldIndices)

        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }

    private fun normalizeIndices(oldIndices: Map<TokenId, Int>) {
        val allTokens = ServerContext.modelRepository.findAllTokens()
        allTokens
            .sortedWith(indexComparator(oldIndices))
            .forEachIndexed { i, token -> token.index.value = i }
    }

    private fun indexComparator(oldIndices: Map<TokenId, Int>): Comparator<TokenModel> =
        Comparator { a, b ->
            val diff = a.index.value - b.index.value
            if (diff != 0) return@Comparator diff
            // Tie-break: a token that moved in a direction belongs on that side of unchanged tokens.
            val aDelta = oldIndices[a.id]?.let { a.index.value - it }
            val bDelta = oldIndices[b.id]?.let { b.index.value - it }
            when {
                aDelta != null && bDelta != null -> 0          // both adjusted: stable order
                aDelta != null && aDelta > 0     -> 1          // a moved up: a after b
                aDelta != null && aDelta < 0     -> -1         // a moved down: a before b
                bDelta != null && bDelta > 0     -> -1         // b moved up: b after a
                bDelta != null && bDelta < 0     -> 1          // b moved down: b before a
                else                             -> 0
            }
        }
}
