package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.Pointer

data class PointersUpdatedEvent(val pointers: List<Pointer>) : Event {
    companion object { private const val serialVersionUID = 1L }
}
