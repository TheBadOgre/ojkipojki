package net.rafkos.ojkipojki.shared

import org.apache.logging.log4j.LogManager
import java.io.ObjectInputStream
import java.net.Socket

abstract class Receiver<A : Any>(
    socket: Socket,
    private val dispatcher: Dispatcher<A>,
    private val onDisconnected: (() -> Unit)? = null
) {
    private val inputStream = ObjectInputStream(socket.getInputStream())
    @Volatile private var running = true

    fun start() {
        Thread {
            while (running) {
                val item = receive()
                if (item != null) {
                    log.info("Received: ${item.javaClass.simpleName}")
                    dispatcher.dispatch(item)
                } else {
                    running = false
                    onDisconnected?.invoke()
                }
            }
        }.start()
    }

    fun receive(): A? = try {
        @Suppress("UNCHECKED_CAST")
        inputStream.readObject() as? A
    } catch (e: Exception) {
        null
    }

    fun shutdown() {
        running = false
    }

    companion object {
        private val log = LogManager.getLogger(Receiver::class.java)
    }
}