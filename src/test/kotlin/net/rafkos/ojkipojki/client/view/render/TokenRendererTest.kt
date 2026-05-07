package net.rafkos.ojkipojki.client.view.render

import net.rafkos.ojkipojki.shared.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class TokenRendererTest {
    private val renderer = TokenRenderer()
    private fun token(x: Int = 0, y: Int = 0, rot: Double = 0.0): Token =
        Token(TokenId(UUID.randomUUID()), SpriteId(SpriteBagId("b"), 1, 0, 0),
            Position(x, y), Rotation(rot), Index(0))

    @Test
    fun `missingHitTest returns true for point at center`() {
        val t = token(100, 200)
        assertTrue(renderer.missingHitTest(t, 100.0, 200.0))
    }

    @Test
    fun `missingHitTest returns false for point well outside box`() {
        val t = token(100, 200)
        assertFalse(renderer.missingHitTest(t, 300.0, 200.0))
    }

    @Test
    fun `missingHitTest accounts for rotation`() {
        // Token at origin, rotated 90 degrees. A point within 40 units of center
        // should be inside the 80x80 box regardless of rotation.
        val t = token(0, 0, 90.0)
        assertTrue(renderer.missingHitTest(t, 35.0, 0.0))
    }
}
