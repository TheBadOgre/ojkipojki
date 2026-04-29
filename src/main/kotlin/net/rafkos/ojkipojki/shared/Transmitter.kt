package net.rafkos.ojkipojki.shared

import java.io.ObjectOutputStream
import java.net.Socket

abstract class Transmitter<A : Any>(private val socket: Socket) {
    private val outputStream = ObjectOutputStream(socket.getOutputStream())

    fun transmit(obj: A) {
        outputStream.writeObject(obj)
        outputStream.flush()
    }
}
