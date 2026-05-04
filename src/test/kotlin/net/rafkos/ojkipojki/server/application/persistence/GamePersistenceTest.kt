package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.model.TokenModel
import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.UUID
import java.util.zip.GZIPOutputStream

class GamePersistenceTest {

    @TempDir
    lateinit var tempDir: File

    private fun tmpFile() = File(tempDir, "test_${UUID.randomUUID()}.sav")

    private fun repoWithTokens(vararg tokens: Token): ModelRepository {
        val repo = ModelRepository()
        tokens.forEach { repo.saveToken(TokenModel().apply { apply(it) }) }
        return repo
    }

    private fun token(
        spriteId: SpriteId = SpriteId(SpriteBagId("bag"), 1, 2, 3),
        position: Position = Position(10, 20),
        rotation: Rotation = Rotation(30.0),
        index: Index = Index(2),
        flipped: Boolean = false,
        locked: Boolean = false,
    ) = Token(TokenId(UUID.randomUUID()), spriteId, position, rotation, index, flipped, locked)

    @Test
    fun `round-trip preserves all token fields`() {
        val original = token(
            spriteId = SpriteId(SpriteBagId("dice"), 10, 20, 30),
            position = Position(100, 200),
            rotation = Rotation(45.0),
            index = Index(7),
            flipped = true,
            locked = true,
        )
        val file = tmpFile()

        GamePersistence.save(repoWithTokens(original), file)
        val loaded = GamePersistence.load(file)

        assertEquals(1, loaded.tokens.size)
        val t = loaded.tokens.first()
        assertEquals(original.id, t.id)
        assertEquals(original.spriteId, t.spriteId)
        assertEquals(original.position, t.position)
        assertEquals(original.rotation, t.rotation)
        assertEquals(original.index, t.index)
        assertEquals(original.flipped, t.flipped)
        assertEquals(original.locked, t.locked)
    }

    @Test
    fun `round-trip with spriteId containing colon in bag name`() {
        val spriteId = SpriteId(SpriteBagId("my:bag"), 1, 2, 3)
        val file = tmpFile()

        GamePersistence.save(repoWithTokens(token(spriteId = spriteId)), file)
        val loaded = GamePersistence.load(file)

        assertEquals(spriteId, loaded.tokens.first().spriteId)
    }

    @Test
    fun `empty repository round-trip returns empty collections`() {
        val file = tmpFile()

        GamePersistence.save(ModelRepository(), file)
        val loaded = GamePersistence.load(file)

        assertTrue(loaded.tokens.isEmpty())
        assertTrue(loaded.spriteBags.isEmpty())
    }

    @Test
    fun `spriteBags always empty after load`() {
        val file = tmpFile()
        GamePersistence.save(repoWithTokens(token()), file)

        val loaded = GamePersistence.load(file)

        assertTrue(loaded.spriteBags.isEmpty())
    }

    @Test
    fun `multiple tokens round-trip`() {
        val tokens = (1..10).map { token() }
        val file = tmpFile()

        GamePersistence.save(repoWithTokens(*tokens.toTypedArray()), file)
        val loaded = GamePersistence.load(file)

        val originalIds = tokens.map { it.id }.toSet()
        val loadedIds = loaded.tokens.map { it.id }.toSet()
        assertEquals(originalIds, loadedIds)
    }

    @Test
    fun `save file is GZIP compressed`() {
        val file = tmpFile()
        GamePersistence.save(ModelRepository(), file)

        val bytes = file.readBytes()
        // GZIP magic number: 0x1f 0x8b
        assertEquals(0x1f, bytes[0].toInt() and 0xFF)
        assertEquals(0x8b, bytes[1].toInt() and 0xFF)
    }

    @Test
    fun `load unknown format throws IllegalArgumentException`() {
        val file = tmpFile()
        ObjectOutputStream(GZIPOutputStream(FileOutputStream(file))).use {
            it.writeObject("not a GameSave object")
        }

        assertThrows(IllegalArgumentException::class.java) {
            GamePersistence.load(file)
        }
    }
}
