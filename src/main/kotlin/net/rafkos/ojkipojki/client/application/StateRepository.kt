package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.domain.Sprite
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import java.util.concurrent.ConcurrentHashMap

class StateRepository {

    private val spriteBags = ConcurrentHashMap<SpriteBagId, SpriteBag>()
    private val sprites = ConcurrentHashMap<SpriteId, Sprite>()
    private val tokens = ConcurrentHashMap<TokenId, Token>()
    private val pointers = ConcurrentHashMap<Triple<Int, Int, Int>, Pointer>()

    fun findSpriteBagById(id: SpriteBagId): SpriteBag? = spriteBags[id]
    fun findAllSpriteBags(): List<SpriteBag> = spriteBags.values.toList()
    fun saveSpriteBag(spriteBag: SpriteBag) { spriteBags[spriteBag.id] = spriteBag }

    fun findSpriteById(id: SpriteId): Sprite? = sprites[id]
    fun findAllSprites(): List<Sprite> = sprites.values.toList()
    fun saveSprite(sprite: Sprite) { sprites[sprite.id] = sprite }

    fun findTokenById(id: TokenId): Token? = tokens[id]
    fun findAllTokens(): List<Token> = tokens.values.toList()
    fun saveToken(token: Token) { tokens[token.id] = token }
    fun deleteToken(id: TokenId) { tokens.remove(id) }
    fun replaceAllTokens(incoming: List<Token>) {
        tokens.clear()
        incoming.forEach { tokens[it.id] = it }
    }

    fun findAllPointers(): List<Pointer> = pointers.values.toList()
    fun replaceAllPointers(incoming: List<Pointer>) {
        pointers.clear()
        incoming.forEach { pointers[Triple(it.red, it.green, it.blue)] = it }
    }
}
