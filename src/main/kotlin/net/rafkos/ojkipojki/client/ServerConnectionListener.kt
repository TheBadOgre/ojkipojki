package net.rafkos.ojkipojki.client

import java.net.Socket

interface ServerConnectionListener {
    fun onConnected(socket: Socket)
    fun onDisconnected()
}
