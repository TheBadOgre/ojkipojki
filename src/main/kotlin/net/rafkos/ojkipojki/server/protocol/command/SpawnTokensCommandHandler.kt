package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import java.util.UUID
import kotlin.random.Random

class SpawnTokensCommandHandler : Handler<SpawnTokensCommand> {
    override fun handle(action: SpawnTokensCommand) {
        val bag = ServerContext.modelRepository.findSpriteBagById(action.spriteBagId) ?: return
        val sprites = if (action.spriteId != null) bag.sprites.filter { it.id == action.spriteId } else bag.sprites
        val baseIndex = (ServerContext.modelRepository.findAllTokens().maxOfOrNull { it.index.value } ?: -1) + 1
        val spawnedIds = mutableSetOf<TokenId>()
        sprites.forEachIndexed { i, sprite ->
            val model = TokenModel()
            model.id = TokenId(UUID.randomUUID())
            model.spriteId = sprite.id
            val baseX = action.position?.x ?: Random.nextInt(-100, 101)
            val baseY = action.position?.y ?: 0
            model.position.apply(Position(x = baseX, y = i * 20 + baseY))
            model.index.value = baseIndex + i
            ServerContext.modelRepository.saveToken(model)
            spawnedIds.add(model.id)
        }
        val tokenActions = ServerContext.modelRepository.findAllTokens()
            .filter { it.id in spawnedIds }
            .map { SomeTokensUpdatedEvent.TokenAction.Update(it.toState()) }
        ServerContext.eventBroadcastService.broadcast(SomeTokensUpdatedEvent(tokenActions))
    }
}
