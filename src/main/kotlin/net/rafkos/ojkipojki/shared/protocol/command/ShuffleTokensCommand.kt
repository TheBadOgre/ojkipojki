package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.TokenId

data class ShuffleTokensCommand(val tokenIds: List<TokenId>) : Command
