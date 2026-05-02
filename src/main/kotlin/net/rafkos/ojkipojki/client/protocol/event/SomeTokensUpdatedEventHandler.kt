package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent

class SomeTokensUpdatedEventHandler : Handler<SomeTokensUpdatedEvent> {
    override fun handle(action: SomeTokensUpdatedEvent) {
        action.tokenActions.forEach { tokenAction ->
            when (tokenAction) {
                is SomeTokensUpdatedEvent.TokenAction.Update -> ClientContext.stateRepository.saveToken(tokenAction.token)
                is SomeTokensUpdatedEvent.TokenAction.Delete -> ClientContext.stateRepository.deleteToken(tokenAction.tokenId)
            }
        }
        ClientContext.onTokensUpdated?.invoke()
    }
}
