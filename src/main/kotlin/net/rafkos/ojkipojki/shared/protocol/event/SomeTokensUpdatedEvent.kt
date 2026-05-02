package net.rafkos.ojkipojki.shared.protocol.event

import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.io.Serializable

data class SomeTokensUpdatedEvent(val tokenActions: List<TokenAction>) : Event {
    sealed class TokenAction : Serializable {
        data class Update(val token: Token) : TokenAction()
        data class Delete(val tokenId: TokenId) : TokenAction()
    }
}
