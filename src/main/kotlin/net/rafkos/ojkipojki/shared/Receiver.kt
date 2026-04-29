package net.rafkos.ojkipojki.shared

import net.rafkos.ojkipojki.client.event.EventReceiver
import org.apache.logging.log4j.LogManager
import java.io.ObjectInputStream
import java.net.Socket

abstract class Receiver<A : Any>(
    socket: Socket,
    private val eventDispatcher: Dispatcher<A>,
) {
    private val inputStream = ObjectInputStream(socket.getInputStream())
    private var running = true

    fun start() {
        Thread {
            while (running) {
                val event = receive()
                if (event != null) {
                    log.info("Received event: ${event.javaClass.simpleName}")
                    eventDispatcher.dispatch(event)
                } else {
                    running = false
                }
            }
        }.start()
    }

    fun receive(): A? {
        return try {
            inputStream.readObject() as? A
        } catch (e: Exception) {
            null
        }
    }

    fun shutdown() {
        running = false
    }

    companion object {
        private val log = LogManager.getLogger(EventReceiver::class.java)
    }
}
