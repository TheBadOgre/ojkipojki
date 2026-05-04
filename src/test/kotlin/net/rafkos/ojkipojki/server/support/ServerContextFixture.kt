package net.rafkos.ojkipojki.server.support

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.application.ClientColorRegistry
import net.rafkos.ojkipojki.server.application.CommandContext
import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.server.application.PointerRepository
import net.rafkos.ojkipojki.server.protocol.command.CommandDispatcher
import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import org.mockito.kotlin.mock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ServerContextFixture(
    val modelRepository: ModelRepository,
    val pointerRepository: PointerRepository,
    val clientColorRegistry: ClientColorRegistry,
    val eventBroadcastService: EventBroadcastService,
    val commandDispatcher: CommandDispatcher,
    val commandExecutor: ExecutorService,
)

fun serverContextFixture(): ServerContextFixture {
    val fixture = ServerContextFixture(
        modelRepository = ModelRepository(),
        pointerRepository = PointerRepository(),
        clientColorRegistry = ClientColorRegistry(),
        eventBroadcastService = mock(),
        commandDispatcher = mock(),
        commandExecutor = Executors.newSingleThreadExecutor(),
    )
    ServerContext.modelRepository = fixture.modelRepository
    ServerContext.pointerRepository = fixture.pointerRepository
    ServerContext.clientColorRegistry = fixture.clientColorRegistry
    ServerContext.eventBroadcastService = fixture.eventBroadcastService
    ServerContext.commandDispatcher = fixture.commandDispatcher
    ServerContext.commandExecutor = fixture.commandExecutor
    return fixture
}

fun resetCommandContext() {
    CommandContext.clientId = null
}
