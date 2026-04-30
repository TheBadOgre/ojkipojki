package net.rafkos.ojkipojki.server.protocol

import java.net.Socket

interface ClientConnectionListener {
    fun onClientConnected(clientId: String, socket: Socket)
    fun onClientDisconnected(clientId: String)
}
