package net.rafkos.ojkipojki.server.application

import java.util.concurrent.ConcurrentHashMap

class ClientColorRegistry {

    private val colorPool = listOf(
        Triple(220, 50,  50),   Triple(50,  150, 220),  Triple(50,  180, 80),   Triple(220, 150, 30),
        Triple(180, 60,  220),  Triple(40,  200, 200),  Triple(220, 60,  150),  Triple(180, 220, 30),
        Triple(100, 80,  220),  Triple(220, 200, 40),   Triple(30,  100, 180),  Triple(180, 100, 40),
        Triple(40,  180, 150),  Triple(220, 100, 80),   Triple(100, 220, 150),  Triple(180, 40,  80),
        Triple(80,  220, 220),  Triple(220, 130, 180),  Triple(130, 100, 180),  Triple(180, 220, 100),
        Triple(40,  130, 100),  Triple(220, 60,  60),   Triple(60,  60,  220),  Triple(60,  200, 60),
        Triple(220, 180, 60),   Triple(160, 40,  180),  Triple(40,  180, 220),  Triple(200, 100, 40),
        Triple(40,  220, 120),  Triple(220, 40,  180),  Triple(120, 220, 40),   Triple(40,  80,  220),
    )

    private val assignments = ConcurrentHashMap<String, Triple<Int, Int, Int>>()

    @Synchronized
    fun assign(clientId: String) {
        val used = assignments.values.toSet()
        val color = colorPool.firstOrNull { it !in used } ?: colorPool.first()
        assignments[clientId] = color
    }

    fun release(clientId: String) {
        assignments.remove(clientId)
    }

    fun getColor(clientId: String): Triple<Int, Int, Int>? = assignments[clientId]
}
