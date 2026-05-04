package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.shared.domain.Pointer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PointerRepositoryTest {

    private lateinit var repo: PointerRepository

    @BeforeEach
    fun setup() {
        repo = PointerRepository()
    }

    private fun pointer(x: Int = 0, y: Int = 0) = Pointer(x, y, 255, 0, 0)

    @Test
    fun `save and findAllExcept excludes saved client`() {
        repo.save("c1", pointer(10, 20))
        repo.save("c2", pointer(30, 40))
        val result = repo.findAllExcept("c1")
        assertEquals(1, result.size)
        assertEquals(Pointer(30, 40, 255, 0, 0), result.first())
    }

    @Test
    fun `findAllExcept with unknown id returns all`() {
        repo.save("c1", pointer())
        assertEquals(1, repo.findAllExcept("nobody").size)
    }

    @Test
    fun `remove deletes pointer`() {
        repo.save("c1", pointer())
        repo.remove("c1")
        assertTrue(repo.findAllExcept("nobody").isEmpty())
    }

    @Test
    fun `remove unknown id is no-op`() {
        assertDoesNotThrow { repo.remove("ghost") }
    }

    @Test
    fun `saving same client id overwrites pointer`() {
        repo.save("c1", Pointer(1, 2, 10, 20, 30))
        repo.save("c1", Pointer(9, 8, 10, 20, 30))
        val result = repo.findAllExcept("nobody")
        assertEquals(1, result.size)
        assertEquals(Pointer(9, 8, 10, 20, 30), result.first())
    }
}
