package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.Rotation

class RotationModel {
    var degrees: Double = 0.0
    val radians: Double
        get() = Math.toRadians(degrees)

    fun apply(state: Rotation) {
        this.degrees = state.degrees
    }

    fun toState(): Rotation = Rotation(degrees)
}
