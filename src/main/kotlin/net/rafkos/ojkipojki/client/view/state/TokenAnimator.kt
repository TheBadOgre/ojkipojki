package net.rafkos.ojkipojki.client.view.state

import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

class TokenAnimator {
    companion object {
        private const val FACTOR    = 0.25
        private const val FLIP_STEP = 1.0 / 9.0    // ~150ms at 60fps
    }

    private data class VisualState(var x: Double, var y: Double, var rotation: Double)
    private data class FlipAnim(val horizontal: Boolean, var progress: Double = 0.0, val targetFlipped: Boolean)

    private val states       = mutableMapOf<TokenId, VisualState>()
    private val immediateIds = mutableSetOf<TokenId>()
    private val knownFlipped = mutableMapOf<TokenId, Boolean>()
    private val flipAnims    = mutableMapOf<TokenId, FlipAnim>()

    fun syncWithTokens(tokens: List<Token>) {
        val live = tokens.map { it.id }.toSet()
        states.keys.retainAll(live)
        knownFlipped.keys.retainAll(live)
        flipAnims.keys.retainAll(live)

        for (token in tokens) {
            if (token.id !in states) {
                states[token.id] = VisualState(
                    token.position.x.toDouble(),
                    token.position.y.toDouble(),
                    token.rotation.degrees,
                )
            }
            val prev = knownFlipped[token.id]
            if (prev != null && prev != token.flipped && token.id !in flipAnims) {
                flipAnims[token.id] = FlipAnim(
                    horizontal    = Random.nextBoolean(),
                    progress      = 0.0,
                    targetFlipped = token.flipped,
                )
            }
            knownFlipped[token.id] = token.flipped
        }
    }

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

            if (abs(s.x - tx) < 0.5) s.x        = tx
            if (abs(s.y - ty) < 0.5) s.y        = ty
            if (abs(dr)       < 0.5) s.rotation = tr
        }

        val iter = flipAnims.entries.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            e.value.progress = (e.value.progress + FLIP_STEP).coerceAtMost(1.0)
            if (e.value.progress >= 1.0) iter.remove()
        }
    }

    fun visualize(token: Token): Token {
        if (token.id in immediateIds) return token
        val s    = states[token.id] ?: return token
        val anim = flipAnims[token.id]
        // Show old face during first half of animation, new face during second half
        val displayFlipped = if (anim != null && anim.progress < 0.5) !anim.targetFlipped else token.flipped
        return token.copy(
            position = Position(s.x.roundToInt(), s.y.roundToInt()),
            rotation = Rotation(s.rotation),
            flipped  = displayFlipped,
        )
    }

    /** (scaleX, scaleY) for the flip animation; (1,1) when idle. */
    fun flipScale(id: TokenId): Pair<Double, Double> {
        val anim  = flipAnims[id] ?: return 1.0 to 1.0
        val scale = abs(cos(anim.progress * PI))
        return if (anim.horizontal) scale to 1.0 else 1.0 to scale
    }

    fun setImmediate(ids: Collection<TokenId>) { immediateIds.clear(); immediateIds.addAll(ids) }
    fun clearImmediate() { immediateIds.clear() }

    private fun shortestDelta(from: Double, to: Double): Double {
        var d = (to - from) % 360.0
        if (d > 180.0)  d -= 360.0
        if (d < -180.0) d += 360.0
        return d
    }
}
