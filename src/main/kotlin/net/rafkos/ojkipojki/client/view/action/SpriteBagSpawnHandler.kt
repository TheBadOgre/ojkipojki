package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.client.application.StateRepository
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
    private val stateRepository: StateRepository,
) {
    fun spawn(bagId: SpriteBagId, position: Position?, count: Int = 1) {
        transmitter.transmit(SpawnTokensCommand(bagId, position, count = count))
    }

    fun spawnSprite(spriteId: SpriteId, position: Position?, count: Int = 1) {
        transmitter.transmit(SpawnTokensCommand(spriteId.spriteBagId, position, spriteId, count))
    }

    fun spawnGroup(groupName: String, position: Position?, count: Int = 1) {
        stateRepository.findAllSpriteBags()
            .filter { it.groupName == groupName }
            .forEach { bag -> transmitter.transmit(SpawnTokensCommand(bag.id, position, count = count)) }
    }

    fun encodeBag(bagId: SpriteBagId) = "bag:${bagId.id}"
    fun encodeGroup(groupName: String) = "group:$groupName"
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
                data.startsWith("group:") -> spawnGroup(data.removePrefix("group:"), worldPos)
                else -> spawn(SpriteBagId(data), worldPos)
            }
            return true
        }
    }
}
