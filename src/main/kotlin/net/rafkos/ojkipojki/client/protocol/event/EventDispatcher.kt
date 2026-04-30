package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.Event
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent

class EventDispatcher : Dispatcher<Event>(
    mapOf(
        SpriteBagsUpdatedEvent::class to SpriteBagsUpdatedEventHandler() as Handler<Event>,
    )
)
