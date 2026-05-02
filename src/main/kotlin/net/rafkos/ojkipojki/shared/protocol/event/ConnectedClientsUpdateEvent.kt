package net.rafkos.ojkipojki.shared.protocol.event

data class ConnectedClientsUpdateEvent(val numOfClients: Int) : Event {
    companion object { private const val serialVersionUID = 1L }
}