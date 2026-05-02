package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class Rotation(val degrees: Double) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}