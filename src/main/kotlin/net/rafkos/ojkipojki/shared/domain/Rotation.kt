package org.example.net.rafkos.ojkipojki.shared.domain

data class Rotation(val degrees: Double) {
    val radians: Double
        get() = Math.toRadians(degrees)
}