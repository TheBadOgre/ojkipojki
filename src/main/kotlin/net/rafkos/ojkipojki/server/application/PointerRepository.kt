package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.shared.domain.Pointer
import java.util.concurrent.ConcurrentHashMap

class PointerRepository {
    private val pointers = ConcurrentHashMap<String, Pointer>()

    fun save(clientId: String, pointer: Pointer) { pointers[clientId] = pointer }
    fun remove(clientId: String) { pointers.remove(clientId) }
    fun findAllExcept(clientId: String): List<Pointer> =
        pointers.filterKeys { it != clientId }.values.toList()
}
