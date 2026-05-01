package net.rafkos.ojkipojki.client.view.state

import net.rafkos.ojkipojki.shared.domain.Pointer
import kotlin.math.abs

class PointerAnimator {
    companion object { private const val FACTOR = 0.25 }

    private data class VisualPos(var x: Double, var y: Double)
    private val states = mutableMapOf<Triple<Int, Int, Int>, VisualPos>()

    fun syncWithPointers(pointers: List<Pointer>) {
        val live = pointers.map { Triple(it.red, it.green, it.blue) }.toSet()
        states.keys.retainAll(live)
        for (p in pointers) {
            val key = Triple(p.red, p.green, p.blue)
            if (key !in states) states[key] = VisualPos(p.x.toDouble(), p.y.toDouble())
        }
    }

    fun tick(pointers: List<Pointer>) {
        for (p in pointers) {
            val s = states[Triple(p.red, p.green, p.blue)] ?: continue
            val tx = p.x.toDouble()
            val ty = p.y.toDouble()
            s.x += (tx - s.x) * FACTOR
            s.y += (ty - s.y) * FACTOR
            if (abs(s.x - tx) < 0.5) s.x = tx
            if (abs(s.y - ty) < 0.5) s.y = ty
        }
    }

    fun visualize(pointer: Pointer): Pair<Double, Double> {
        val s = states[Triple(pointer.red, pointer.green, pointer.blue)]
            ?: return pointer.x.toDouble() to pointer.y.toDouble()
        return s.x to s.y
    }
}
