package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class Pointer(
    val x: Int,
    val y: Int,
    val red: Int,
    val green: Int,
    val blue: Int
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
