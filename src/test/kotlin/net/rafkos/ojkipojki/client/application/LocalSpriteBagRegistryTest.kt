package net.rafkos.ojkipojki.client.application

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalSpriteBagRegistryTest {

    @Test
    fun `bags returns empty list before reload`() {
        val registry = LocalSpriteBagRegistry()
        assertTrue(registry.bags().isEmpty())
    }

    @Test
    fun `images returns empty map before reload`() {
        val registry = LocalSpriteBagRegistry()
        assertTrue(registry.images().isEmpty())
    }
}
