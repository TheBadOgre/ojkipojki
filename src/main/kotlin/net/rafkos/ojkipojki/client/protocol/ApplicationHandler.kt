package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.application.SpriteLoader
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagCommand
import java.io.File

class ApplicationHandler {
    fun onSessionReady(transmitter: CommandTransmitter) {
        val spriteBags = SpriteLoader.loadSprites(File("E:\\workspace\\ojkipojki\\armies\\test_army"))

        transmitter.transmit(UploadSpriteBagCommand(spriteBags))
    }

    fun onSessionClosed() {

    }
}
