package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.SpriteBagId

data class MissingSpriteBagsEvent(val ids: Set<SpriteBagId>) : Event {
    companion object { private const val serialVersionUID = 1L }
}
