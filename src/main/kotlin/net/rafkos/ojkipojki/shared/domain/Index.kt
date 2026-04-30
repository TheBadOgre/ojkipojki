package net.rafkos.ojkipojki.shared.domain

data class Index(val value: Int) {
    init {
        require(value >= 0) { "Index value must be non-negative: $value" }
    }
}
