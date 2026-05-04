package net.rafkos.ojkipojki.server.application

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClientColorRegistryTest {

    private lateinit var registry: ClientColorRegistry

    @BeforeEach
    fun setup() {
        registry = ClientColorRegistry()
    }

    @Test
    fun `assign gives a non-null color`() {
        registry.assign("c1")
        assertNotNull(registry.getColor("c1"))
    }

    @Test
    fun `getColor returns null before assign`() {
        assertNull(registry.getColor("c1"))
    }

    @Test
    fun `two clients get distinct colors`() {
        registry.assign("c1")
        registry.assign("c2")
        assertNotEquals(registry.getColor("c1"), registry.getColor("c2"))
    }

    @Test
    fun `release removes color`() {
        registry.assign("c1")
        registry.release("c1")
        assertNull(registry.getColor("c1"))
    }

    @Test
    fun `released color can be reassigned to new client`() {
        registry.assign("c1")
        val c1Color = registry.getColor("c1")
        registry.release("c1")
        registry.assign("c2")
        assertEquals(c1Color, registry.getColor("c2"))
    }

    @Test
    fun `assign fills pool without throwing when many clients connect`() {
        repeat(33) { i -> registry.assign("client-$i") }
        // 33rd client gets first pool color (pool wraps)
        assertNotNull(registry.getColor("client-32"))
    }
}
