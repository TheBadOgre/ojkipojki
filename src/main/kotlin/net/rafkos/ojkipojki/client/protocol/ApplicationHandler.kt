package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.application.SpriteLoader
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import java.io.File

class ApplicationHandler {
    fun onSessionReady(transmitter: CommandTransmitter) {
        val spriteBags = SpriteLoader.loadSprites(File("E:\\workspace\\ojkipojki\\armies\\test_army"))

        transmitter.transmit(UploadSpriteBagsCommand(spriteBags))
        spriteBags.forEach { transmitter.transmit(SpawnTokensCommand(it.id)) }
    }

    fun onSessionClosed() {

    }
}
