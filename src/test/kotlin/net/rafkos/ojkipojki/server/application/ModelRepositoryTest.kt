package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.SpriteBagId
import net.rafkos.ojkipojki.shared.domain.SpriteId
import net.rafkos.ojkipojki.shared.domain.TokenId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ModelRepositoryTest {

    private lateinit var repo: ModelRepository

    @BeforeEach
    fun setup() {
        repo = ModelRepository()
    }

    @Test
    fun `saveSpriteBag and findSpriteBagById`() {
        val model = SpriteBagModel().apply { id = SpriteBagId("dice") }
        repo.saveSpriteBag(model)
        assertSame(model, repo.findSpriteBagById(SpriteBagId("dice")))
    }

    @Test
    fun `findSpriteBagById returns null for missing id`() {
        assertNull(repo.findSpriteBagById(SpriteBagId("missing")))
    }

    @Test
    fun `findAllSpriteBags returns all saved bags`() {
        repo.saveSpriteBag(SpriteBagModel().apply { id = SpriteBagId("a") })
        repo.saveSpriteBag(SpriteBagModel().apply { id = SpriteBagId("b") })
        assertEquals(2, repo.findAllSpriteBags().size)
    }

    @Test
    fun `deleteAllSpriteBags clears all bags`() {
        repo.saveSpriteBag(SpriteBagModel().apply { id = SpriteBagId("a") })
        repo.deleteAllSpriteBags()
        assertTrue(repo.findAllSpriteBags().isEmpty())
    }

    @Test
    fun `saveToken and findTokenById`() {
        val model = TokenModel().also { it.id = TokenId(UUID.randomUUID()) }
        repo.saveToken(model)
        assertSame(model, repo.findTokenById(model.id))
    }

    @Test
    fun `findTokenById returns null for missing id`() {
        assertNull(repo.findTokenById(TokenId(UUID.randomUUID())))
    }

    @Test
    fun `deleteToken removes a token`() {
        val model = TokenModel().also { it.id = TokenId(UUID.randomUUID()) }
        repo.saveToken(model)
        repo.deleteToken(model.id)
        assertNull(repo.findTokenById(model.id))
    }

    @Test
    fun `deleteAllTokens removes all tokens`() {
        repo.saveToken(TokenModel().also { it.id = TokenId(UUID.randomUUID()) })
        repo.saveToken(TokenModel().also { it.id = TokenId(UUID.randomUUID()) })
        repo.deleteAllTokens()
        assertTrue(repo.findAllTokens().isEmpty())
    }

    @Test
    fun `findAllTokens returns all saved tokens`() {
        repeat(3) { repo.saveToken(TokenModel().also { it.id = TokenId(UUID.randomUUID()) }) }
        assertEquals(3, repo.findAllTokens().size)
    }

    @Test
    fun `saving token with same id overwrites`() {
        val id = TokenId(UUID.randomUUID())
        val first = TokenModel().also { it.id = id; it.flipped = false }
        val second = TokenModel().also { it.id = id; it.flipped = true }
        repo.saveToken(first)
        repo.saveToken(second)
        assertSame(second, repo.findTokenById(id))
    }

    @Test
    fun `missingBagIds returns empty when no tokens`() {
        assertEquals(emptySet<SpriteBagId>(), repo.missingBagIds())
    }

    @Test
    fun `missingBagIds returns empty when all referenced bags present`() {
        val bagId = SpriteBagId("dice")
        repo.saveSpriteBag(SpriteBagModel().apply { id = bagId })
        val token = TokenModel().also { it.id = TokenId(UUID.randomUUID()); it.spriteId = SpriteId(bagId, 1, 0, 0) }
        repo.saveToken(token)
        assertEquals(emptySet<SpriteBagId>(), repo.missingBagIds())
    }

    @Test
    fun `missingBagIds returns referenced bags absent from repo`() {
        val bagId = SpriteBagId("missing")
        val token = TokenModel().also { it.id = TokenId(UUID.randomUUID()); it.spriteId = SpriteId(bagId, 1, 0, 0) }
        repo.saveToken(token)
        assertEquals(setOf(bagId), repo.missingBagIds())
    }

    @Test
    fun `missingBagIds deduplicates ids referenced by multiple tokens`() {
        val bagId = SpriteBagId("dup")
        repeat(3) { i ->
            repo.saveToken(TokenModel().also { it.id = TokenId(UUID.randomUUID()); it.spriteId = SpriteId(bagId, i, 0, 0) })
        }
        assertEquals(setOf(bagId), repo.missingBagIds())
    }
}
