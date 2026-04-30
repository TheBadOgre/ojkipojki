package net.rafkos.ojkipojki.client.protocol

import java.net.Socket

interface ServerConnectionListener {
    fun onConnected(socket: Socket)
    fun onDisconnected()
}
