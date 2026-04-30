package net.rafkos.ojkipojki.client.event

import net.rafkos.ojkipojki.shared.Dispatcher
import net.rafkos.ojkipojki.shared.Handler
import net.rafkos.ojkipojki.shared.event.Event
import net.rafkos.ojkipojki.shared.event.ModelChangedEvent

class EventDispatcher : Dispatcher<Event>(
    mapOf(
        ModelChangedEvent::class to ModelChangedEventHandler() as Handler<in Event>
    )
)
