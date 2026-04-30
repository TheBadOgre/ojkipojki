package net.rafkos.ojkipojki.shared.protocol

import org.apache.logging.log4j.LogManager
import java.io.ObjectOutputStream
import java.net.Socket

abstract class Transmitter<A : Any>(socket: Socket) {
    private val outputStream = ObjectOutputStream(socket.getOutputStream())

    @Synchronized
    fun transmit(obj: A) {
        log.info("Transmitting: ${obj.javaClass.simpleName}")
        outputStream.writeObject(obj)
        outputStream.flush()
    }

    companion object {
        private val log = LogManager.getLogger(Transmitter::class.java)
    }
}