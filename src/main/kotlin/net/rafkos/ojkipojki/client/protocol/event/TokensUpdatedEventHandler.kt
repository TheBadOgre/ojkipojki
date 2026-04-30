package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class TokensUpdatedEventHandler : Handler<TokensUpdatedEvent> {
    override fun handle(action: TokensUpdatedEvent) {
        ClientContext.stateRepository.replaceAllTokens(action.tokens)
    }
}
