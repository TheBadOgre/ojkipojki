package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.application.CommandContext
import net.rafkos.ojkipojki.shared.protocol.command.Command
import org.apache.logging.log4j.LogManager
import java.io.ObjectInputStream
import java.net.Socket

class CommandReceiver(
    socket: Socket,
    private val clientId: String,
    private val onDisconnected: (() -> Unit)? = null,
) {
    private val inputStream = ObjectInputStream(socket.getInputStream())
    @Volatile private var running = true

    fun start() {
        Thread {
            CommandContext.clientId = clientId
            while (running) {
                val cmd = try {
                    @Suppress("UNCHECKED_CAST")
                    inputStream.readObject() as? Command
                } catch (e: Exception) { null }

                if (cmd != null) {
                    log.debug("Received: ${cmd.javaClass.simpleName}")

                    ServerContext.commandDispatcher.dispatch(cmd)
                } else {
                    running = false
                    onDisconnected?.invoke()
                }
            }
        }.start()
    }

    fun shutdown() { running = false }

    companion object {
        private val log = LogManager.getLogger(CommandReceiver::class.java)
    }
}
