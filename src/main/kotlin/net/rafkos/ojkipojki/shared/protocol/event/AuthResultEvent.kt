package net.rafkos.ojkipojki.shared.protocol.event

data class AuthResultEvent(val accepted: Boolean) : Event {
    companion object {
        private const val serialVersionUID = 1L
    }
}
