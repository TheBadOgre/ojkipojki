package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.Token

data class TokensUpdatedEvent(val tokens: List<Token>) : Event
