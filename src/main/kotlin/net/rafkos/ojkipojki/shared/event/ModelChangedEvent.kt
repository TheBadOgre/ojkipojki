package net.rafkos.ojkipojki.shared.event

import net.rafkos.ojkipojki.shared.domain.DomainModel

data class ModelChangedEvent(val models: List<DomainModel> = emptyList()) : Event
