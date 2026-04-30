package net.rafkos.ojkipojki.client.event

import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.event.ModelChangedEvent
import org.apache.logging.log4j.LogManager

class ModelChangedEventHandler : Handler<ModelChangedEvent> {

    override fun handle(event: ModelChangedEvent) {
        log.info("Model changed: ${event.models.size} models received")
    }

    companion object {
        private val log = LogManager.getLogger(ModelChangedEventHandler::class.java)
    }
}
