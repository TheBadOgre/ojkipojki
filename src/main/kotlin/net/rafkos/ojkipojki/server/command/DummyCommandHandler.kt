package net.rafkos.ojkipojki.server.command

import org.example.net.rafkos.ojkipojki.server.event.EventBroadcastService
import net.rafkos.ojkipojki.server.model.Model
import net.rafkos.ojkipojki.shared.command.DummyCommand
import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.event.ModelChangedEvent
import org.apache.logging.log4j.LogManager

class DummyCommandHandler(
    private val model: Model,
    private val eventBroadcastService: EventBroadcastService? = null
) : Handler<DummyCommand> {
    private val log = LogManager.getLogger(DummyCommandHandler::class.java)

    override fun handle(action: DummyCommand) {
        log.info("Received dummy command: ${action.message}")
        model.addModel(net.rafkos.ojkipojki.shared.domain.DomainModel("1", "Test", 100))
        val event = ModelChangedEvent(model.getAllModels())
        eventBroadcastService?.broadcast(event)
    }
}