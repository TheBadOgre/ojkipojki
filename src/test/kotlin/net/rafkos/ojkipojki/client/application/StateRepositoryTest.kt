package net.rafkos.ojkipojki.client.application

import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class StateRepositoryTest {

    private lateinit var repo: StateRepository

    @BeforeEach
    fun setup() {
        repo = StateRepository()
    }

    private fun bagId(id: String) = SpriteBagId(id)
    private fun spriteId(bagId: String) = SpriteId(SpriteBagId(bagId), 1, 2, 3)
    private fun bag(id: String) = SpriteBag(bagId(id), "group-$id", emptyList())
    private fun sprite(bagId: String) = Sprite(spriteId(bagId), byteArrayOf(), byteArrayOf())
    private fun tokenId() = TokenId(UUID.randomUUID())
    private fun token(id: TokenId) = Token(id, spriteId("bag"))
    private fun pointer(r: Int, g: Int, b: Int) = Pointer(0, 0, r, g, b)

    @Test
    fun `saveSpriteBag and findSpriteBagById`() {
        val b = bag("dice")
        repo.saveSpriteBag(b)
        assertEquals(b, repo.findSpriteBagById(bagId("dice")))
    }

    @Test
    fun `findSpriteBagById returns null when missing`() {
        assertNull(repo.findSpriteBagById(bagId("missing")))
    }

    @Test
    fun `saveSprite and findSpriteById`() {
        val s = sprite("bag")
        repo.saveSprite(s)
        assertSame(s, repo.findSpriteById(spriteId("bag")))
    }

    @Test
    fun `saveToken and findTokenById`() {
        val id = tokenId()
        val t = token(id)
        repo.saveToken(t)
        assertEquals(t, repo.findTokenById(id))
    }

    @Test
    fun `deleteToken removes it`() {
        val id = tokenId()
        repo.saveToken(token(id))
        repo.deleteToken(id)
        assertNull(repo.findTokenById(id))
    }

    @Test
    fun `replaceAllTokens replaces everything`() {
        val oldId = tokenId()
        repo.saveToken(token(oldId))

        val newId = tokenId()
        repo.replaceAllTokens(listOf(token(newId)))

        assertNull(repo.findTokenById(oldId))
        assertNotNull(repo.findTokenById(newId))
        assertEquals(1, repo.findAllTokens().size)
    }

    @Test
    fun `replaceAllPointers replaces everything keyed by RGB`() {
        repo.replaceAllPointers(listOf(pointer(1, 2, 3)))
        repo.replaceAllPointers(listOf(pointer(4, 5, 6), pointer(7, 8, 9)))

        val result = repo.findAllPointers()
        assertEquals(2, result.size)
        assertTrue(result.any { it.red == 4 })
        assertTrue(result.any { it.red == 7 })
    }

    @Test
    fun `findAllPointers initially empty`() {
        assertTrue(repo.findAllPointers().isEmpty())
    }
}
