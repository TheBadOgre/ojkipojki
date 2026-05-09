package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent

class SpriteBagsUpdatedEventHandler : Handler<SpriteBagsUpdatedEvent> {
    override fun handle(action: SpriteBagsUpdatedEvent) {
        action.spriteBags.forEach { bag ->
            ClientContext.stateRepository.saveSpriteBag(bag)
            bag.sprites.forEach { ClientContext.stateRepository.saveSprite(it) }
        }
        ClientContext.notifier.notifySpriteBagsUpdated()
    }
}
