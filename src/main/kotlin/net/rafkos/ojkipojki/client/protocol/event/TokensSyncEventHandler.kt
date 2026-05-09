package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.TokensSyncEvent

// Fired periodically by the server heartbeat and on first connect. Replaces the full token list.
class TokensSyncEventHandler : Handler<TokensSyncEvent> {
    override fun handle(action: TokensSyncEvent) {
        ClientContext.stateRepository.replaceAllTokens(action.tokens)
        ClientContext.notifier.notifyTokensUpdated()
        ClientContext.notifier.notifyTokensCountChanged()
    }
}
