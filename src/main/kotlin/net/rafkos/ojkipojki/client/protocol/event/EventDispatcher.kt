package net.rafkos.ojkipojki.client.protocol.event

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.event.ConnectedClientsUpdateEvent
import net.rafkos.ojkipojki.shared.protocol.event.Event
import net.rafkos.ojkipojki.shared.protocol.event.GameInitializationEvent
import net.rafkos.ojkipojki.shared.protocol.event.MissingSpriteBagsEvent
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.SomeTokensUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.SpriteBagsUpdatedEvent
import net.rafkos.ojkipojki.shared.protocol.event.TokensSyncEvent

class EventDispatcher : Dispatcher<Event>(
    mapOf(
        SpriteBagsUpdatedEvent::class to SpriteBagsUpdatedEventHandler() as Handler<Event>,
        TokensSyncEvent::class to TokensSyncEventHandler() as Handler<Event>,
        SomeTokensUpdatedEvent::class to SomeTokensUpdatedEventHandler() as Handler<Event>,
        PointersUpdatedEvent::class to PointersUpdatedEventHandler() as Handler<Event>,
        ConnectedClientsUpdateEvent::class to ConnectedClientsUpdateEventHandler() as Handler<Event>,
        GameInitializationEvent::class to GameInitializationEventHandler() as Handler<Event>,
        MissingSpriteBagsEvent::class to MissingSpriteBagsEventHandler() as Handler<Event>,
    )
)
