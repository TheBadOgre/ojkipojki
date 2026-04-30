package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.command.DummyCommand
import org.apache.logging.log4j.LogManager

class DummyCommandHandler(
    private val eventBroadcastService: EventBroadcastService
) : Handler<DummyCommand> {

    override fun handle(action: DummyCommand) {
        log.info("Received dummy command: ${action.message}")
    }

    companion object {
        private val log = LogManager.getLogger(DummyCommandHandler::class.java)
    }
}
