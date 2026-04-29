package net.rafkos.ojkipojki.client.event

import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.event.ModelChangedEvent
import org.apache.logging.log4j.LogManager

class ModelChangedEventHandler : Handler<ModelChangedEvent> {
    private val log = LogManager.getLogger(ModelChangedEventHandler::class.java)

    override fun handle(event: ModelChangedEvent) {
        log.info("Handling model changed event")
        log.info("State updated: ${event.models.size} models")
    }
}
