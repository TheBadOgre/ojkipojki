package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.Position

class PositionModel {
    var x: Int = 0
    var y: Int = 0

    fun apply(state: Position) {
        this.x = state.x
        this.y = state.y
    }

    fun toState(): Position = Position(x, y)
}
