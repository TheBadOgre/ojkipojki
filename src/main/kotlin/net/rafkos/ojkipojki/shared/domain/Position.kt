package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class Position(val x: Int, val y: Int) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
