package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.TokenId

data class LockTokensCommand(val tokenIds: List<TokenId>, val locked: Boolean) : Command
