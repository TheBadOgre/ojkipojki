package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.TokenId

data class DeleteTokensCommand(val tokenIds: List<TokenId>) : Command {
    companion object { private const val serialVersionUID = 1L }
}
