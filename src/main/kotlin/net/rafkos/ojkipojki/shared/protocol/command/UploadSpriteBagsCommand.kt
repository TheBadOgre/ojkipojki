package net.rafkos.ojkipojki.shared.protocol.command

import net.rafkos.ojkipojki.shared.domain.SpriteBag

data class UploadSpriteBagsCommand(val spriteBags: List<SpriteBag>) : Command
