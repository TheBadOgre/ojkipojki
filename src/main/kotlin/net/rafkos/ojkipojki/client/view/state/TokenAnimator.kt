package net.rafkos.ojkipojki.client.view.state

import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import kotlin.math.abs
import kotlin.math.roundToInt

class TokenAnimator {
    companion object { private const val FACTOR = 0.25 }

    private data class VisualState(var x: Double, var y: Double, var rotation: Double)

    private val states       = mutableMapOf<TokenId, VisualState>()
    private val immediateIds = mutableSetOf<TokenId>()

    /** Called on TokensUpdatedEvent (EDT). Initialises new tokens at target; existing states animate. */
    fun syncWithTokens(tokens: List<Token>) {
        val live = tokens.map { it.id }.toSet()
        states.keys.retainAll(live)
        for (token in tokens) {
            if (token.id !in states) {
                states[token.id] = VisualState(
                    token.position.x.toDouble(),
                    token.position.y.toDouble(),
                    token.rotation.degrees,
                )
            }
        }
    }

    /** Advance one animation frame toward current stateRepository values. Call on EDT. */
    fun tick(tokens: List<Token>) {
        for (token in tokens) {
            val s = states[token.id] ?: continue
            if (token.id in immediateIds) {
                s.x        = token.position.x.toDouble()
                s.y        = token.position.y.toDouble()
                s.rotation = token.rotation.degrees
                continue
            }
            val tx = token.position.x.toDouble()
            val ty = token.position.y.toDouble()
            val tr = token.rotation.degrees

            s.x += (tx - s.x) * FACTOR
            s.y += (ty - s.y) * FACTOR
            val dr = shortestDelta(s.rotation, tr)
            s.rotation += dr * FACTOR

            if (abs(s.x - tx) < 0.5)  s.x        = tx
            if (abs(s.y - ty) < 0.5)  s.y        = ty
            if (abs(dr)       < 0.5)  s.rotation = tr
        }
    }

    /** Returns token with animated position/rotation for rendering. */
    fun visualize(token: Token): Token {
        if (token.id in immediateIds) return token
        val s = states[token.id] ?: return token
        return token.copy(
            position = Position(s.x.roundToInt(), s.y.roundToInt()),
            rotation = Rotation(s.rotation),
        )
    }

    /** Bypass animation for dragged tokens so they track the cursor immediately. */
    fun setImmediate(ids: Collection<TokenId>) { immediateIds.clear(); immediateIds.addAll(ids) }
    fun clearImmediate() { immediateIds.clear() }

    private fun shortestDelta(from: Double, to: Double): Double {
        var d = (to - from) % 360.0
        if (d > 180.0)  d -= 360.0
        if (d < -180.0) d += 360.0
        return d
    }
}
