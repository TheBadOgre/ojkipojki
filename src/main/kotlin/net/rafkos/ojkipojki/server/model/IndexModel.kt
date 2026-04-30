package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.Index

class IndexModel {
    var value: Int = 0

    fun apply(state: Index) {
        this.value = state.value
    }

    fun toState(): Index = Index(value)
}
