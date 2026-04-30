package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.SpriteBag

data class UploadSpriteBagCommand(val spriteBags: List<SpriteBag>) : Command
