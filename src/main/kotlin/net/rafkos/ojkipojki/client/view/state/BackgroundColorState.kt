package net.rafkos.ojkipojki.client.view.state

import java.awt.Color

class BackgroundColorState(initial: Color = Color(224, 220, 212)) {
    @Volatile
    var color: Color = initial
        private set

    private val listeners = mutableListOf<(Color) -> Unit>()

    fun addListener(l: (Color) -> Unit) { listeners.add(l) }

    fun setColor(c: Color) {
        color = c
        listeners.forEach { it(c) }
    }
}
