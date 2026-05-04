package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.shared.protocol.Dispatcher
import net.rafkos.ojkipojki.shared.protocol.command.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class CommandDispatcherTest {

    private val expectedCommandTypes: Set<KClass<out Command>> = setOf(
        UploadSpriteBagsCommand::class,
        MoveTokensCommand::class,
        SpawnTokensCommand::class,
        DeleteTokensCommand::class,
        ShuffleTokensCommand::class,
        LockTokensCommand::class,
        MovePointerCommand::class,
    )

    @Test
    fun `CommandDispatcher has handler for every known command type`() {
        val dispatcher = CommandDispatcher()
        val handlersField = Dispatcher::class.java.getDeclaredField("handlers").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val handlers = handlersField.get(dispatcher) as Map<KClass<*>, *>

        expectedCommandTypes.forEach { type ->
            assertTrue(handlers.containsKey(type)) {
                "Missing handler for ${type.simpleName}"
            }
        }
    }
}
