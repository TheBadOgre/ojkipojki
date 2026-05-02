package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.state.ViewportState
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import java.awt.datatransfer.DataFlavor
import javax.swing.JPanel
import javax.swing.TransferHandler

class SpriteBagSpawnHandler(
    private val transmitter: CommandTransmitter,
    private val viewportState: ViewportState,
    private val boardPanel: JPanel,
) {
    fun spawn(bagId: SpriteBagId, position: Position?) {
        transmitter.transmit(SpawnTokensCommand(bagId, position))
    }

    fun spawnSprite(spriteId: SpriteId, position: Position?) {
        transmitter.transmit(SpawnTokensCommand(spriteId.spriteBagId, position, spriteId))
    }

    fun encodeBag(bagId: SpriteBagId) = "bag:${bagId.id}"
    fun encodeSprite(spriteId: SpriteId) = "sprite:${spriteId.spriteBagId.id}:${spriteId.red}:${spriteId.green}:${spriteId.blue}"

    fun createBoardDropHandler(): TransferHandler = object : TransferHandler() {
        override fun canImport(support: TransferSupport) =
            support.isDataFlavorSupported(DataFlavor.stringFlavor)

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) return false
            val data = support.transferable.getTransferData(DataFlavor.stringFlavor) as String
            val dropPt = support.dropLocation.dropPoint
            val worldPos = viewportState.screenToWorld(dropPt, boardPanel.width, boardPanel.height)
            when {
                data.startsWith("sprite:") -> {
                    val parts = data.removePrefix("sprite:").split(":")
                    if (parts.size != 4) return false
                    val spriteId = SpriteId(SpriteBagId(parts[0]), parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
                    spawnSprite(spriteId, worldPos)
                }
                data.startsWith("bag:") -> spawn(SpriteBagId(data.removePrefix("bag:")), worldPos)
                else -> spawn(SpriteBagId(data), worldPos)
            }
            return true
        }
    }
}
