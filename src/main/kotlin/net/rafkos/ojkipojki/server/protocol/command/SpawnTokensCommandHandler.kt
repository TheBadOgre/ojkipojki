package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.TokenId
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.SpawnTokensCommand
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent
import java.util.UUID
import kotlin.random.Random

class SpawnTokensCommandHandler : Handler<SpawnTokensCommand> {
    override fun handle(action: SpawnTokensCommand) {
        val bag = ServerContext.modelRepository.findSpriteBagById(action.spriteBagId) ?: return
        bag.sprites.forEachIndexed { i, sprite ->
            val model = TokenModel()
            model.id = TokenId(UUID.randomUUID())
            model.spriteId = sprite.id
            model.position.apply(Position(x = Random.nextInt(-100, 101), y = i * 20))
            ServerContext.modelRepository.saveToken(model)
        }
        val tokens = ServerContext.modelRepository.findAllTokens().map { it.toState() }
        ServerContext.eventBroadcastService.broadcast(TokensUpdatedEvent(tokens))
    }
}
