package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import net.rafkos.ojkipojki.shared.protocol.event.Event
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.TokensUpdatedEvent

class EventDispatcher : Dispatcher<Event>(
    mapOf(
        SpriteBagsUpdatedEvent::class to SpriteBagsUpdatedEventHandler() as Handler<Event>,
        TokensUpdatedEvent::class to TokensUpdatedEventHandler() as Handler<Event>,
        PointersUpdatedEvent::class to PointersUpdatedEventHandler() as Handler<Event>,
        ConnectedClientsUpdateEvent::class to ConnectedClientsUpdateEventHandler() as Handler<Event>,
    )
)
