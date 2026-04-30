package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.SpriteBag

data class SpriteBagsUpdatedEvent(val spriteBags: List<SpriteBag>) : Event
