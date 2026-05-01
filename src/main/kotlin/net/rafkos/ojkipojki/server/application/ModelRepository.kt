package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.server.model.SpriteModel
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.util.concurrent.ConcurrentHashMap

class ModelRepository {

    private val spriteBags = ConcurrentHashMap<SpriteBagId, SpriteBagModel>()
    private val sprites = ConcurrentHashMap<SpriteId, SpriteModel>()
    private val tokens = ConcurrentHashMap<TokenId, TokenModel>()

    fun findSpriteBagById(id: SpriteBagId): SpriteBagModel? = spriteBags[id]
    fun findAllSpriteBags(): List<SpriteBagModel> = spriteBags.values.toList()
    fun saveSpriteBag(model: SpriteBagModel) { spriteBags[model.id] = model }

    fun findSpriteById(id: SpriteId): SpriteModel? = sprites[id]
    fun findAllSprites(): List<SpriteModel> = sprites.values.toList()
    fun saveSprite(model: SpriteModel) { sprites[model.id] = model }

    fun findTokenById(id: TokenId): TokenModel? = tokens[id]
    fun findAllTokens(): List<TokenModel> = tokens.values.toList()
    fun saveToken(model: TokenModel) { tokens[model.id] = model }
    fun deleteToken(id: TokenId) { tokens.remove(id) }
    fun deleteAllTokens() { tokens.clear() }
}
