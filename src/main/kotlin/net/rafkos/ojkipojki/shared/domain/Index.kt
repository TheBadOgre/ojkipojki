package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class Index(val value: Int) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}
