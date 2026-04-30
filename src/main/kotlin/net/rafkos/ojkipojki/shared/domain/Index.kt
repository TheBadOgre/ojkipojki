package net.rafkos.ojkipojki.shared.domain

import java.io.Serializable

data class Index(val value: Int) : Serializable {
    init {
        require(value >= 0) { "Index value must be non-negative: $value" }
    }
}
