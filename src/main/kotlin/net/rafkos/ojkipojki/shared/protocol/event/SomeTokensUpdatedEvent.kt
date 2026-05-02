package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.io.Serializable

data class SomeTokensUpdatedEvent(val tokenActions: List<TokenAction>) : Event {
    companion object { private const val serialVersionUID = 1L }
    sealed class TokenAction : Serializable {
        companion object { private const val serialVersionUID = 1L }
        data class Update(val token: Token) : TokenAction() {
            companion object { private const val serialVersionUID = 1L }
        }
        data class Delete(val tokenId: TokenId) : TokenAction() {
            companion object { private const val serialVersionUID = 1L }
        }
    }
}
