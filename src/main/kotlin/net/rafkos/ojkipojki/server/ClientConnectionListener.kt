package net.rafkos.ojkipojki.server

import java.net.Socket

interface ClientConnectionListener {
    fun onClientConnected(clientId: String, socket: Socket)
    fun onClientDisconnected(clientId: String)
}
