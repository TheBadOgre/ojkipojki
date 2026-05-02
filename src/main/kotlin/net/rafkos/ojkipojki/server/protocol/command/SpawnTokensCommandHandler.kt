package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

class SpawnTokensCommandHandler : Handler<SpawnTokensCommand> {
    override fun handle(action: SpawnTokensCommand) {
        val bag = ServerContext.modelRepository.findSpriteBagById(action.spriteBagId) ?: return
        val sprites = if (action.spriteId != null) bag.sprites.filter { it.id == action.spriteId } else bag.sprites
        val baseIndex = (ServerContext.modelRepository.findAllTokens().maxOfOrNull { it.index.value } ?: -1) + 1
        val spawnedIds = mutableSetOf<TokenId>()

        val n = sprites.size
        val gridCols = ceil(sqrt(n.toDouble())).toInt().coerceAtLeast(1)
        val gridRows = ceil(n.toDouble() / gridCols).toInt()
        val spacing = 80
        val count = action.count.coerceAtLeast(1)
        val setCols = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)

        for (setIndex in 0 until count) {
            val baseX: Int
            val baseY: Int
            if (action.position != null) {
                val setCol = setIndex % setCols
                val setRow = setIndex / setCols
                baseX = action.position.x + setCol * (gridCols * spacing + spacing)
                baseY = action.position.y + setRow * (gridRows * spacing + spacing)
            } else {
                baseX = Random.nextInt(-400, 401)
                baseY = Random.nextInt(-400, 401)
            }

            sprites.forEachIndexed { i, sprite ->
                val col = i % gridCols
                val row = i / gridCols
                val model = TokenModel()
                model.id = TokenId(UUID.randomUUID())
                model.spriteId = sprite.id
                model.position.apply(Position(x = baseX + col * spacing, y = baseY + row * spacing))
                model.index.value = baseIndex + setIndex * n + i
                ServerContext.modelRepository.saveToken(model)
                spawnedIds.add(model.id)
            }
        }

        val tokenActions = ServerContext.modelRepository.findAllTokens()
            .filter { it.id in spawnedIds }
            .map { SomeTokensUpdatedEvent.TokenAction.Update(it.toState()) }
        ServerContext.eventBroadcastService.broadcast(SomeTokensUpdatedEvent(tokenActions))
    }
}
