package net.rafkos.ojkipojki.shared.protocol.event

import java.io.Serializable

data class GameInitializationEvent(val status: Status, val message: String, val progress: Double = 0.0) : Event {
    init {
        require(progress in 0.0..1.0) { "progress must be between 0.0 and 1.0" }
    }

    enum class Status : Serializable {
        IN_PROGRESS, DONE
    }
}
