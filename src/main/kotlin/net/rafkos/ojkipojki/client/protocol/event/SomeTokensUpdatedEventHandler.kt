package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent

// Fired after every command that modifies specific tokens (move, spawn, delete, shuffle, lock).
// Applies individual update/delete actions rather than replacing the full list.
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
