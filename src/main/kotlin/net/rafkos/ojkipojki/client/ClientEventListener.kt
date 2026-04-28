package net.rafkos.ojkipojki.client

import org.apache.logging.log4j.LogManager
import net.rafkos.ojkipojki.shared.event.Event
import net.rafkos.ojkipojki.shared.event.ModelChangedEvent

class ClientEventListener {
    fun onEvent(event: Event) {
        when (event) {
            is ModelChangedEvent -> {
                log.info("Received model changed event")
            }
        }
    }

    companion object {
        val log = LogManager.getLogger(ClientEventListener::class.java)
    }
}