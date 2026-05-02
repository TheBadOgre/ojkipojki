package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.Token

data class TokensSyncEvent(val tokens: List<Token>) : Event
