package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.protocol.event.EventBroadcastService
import net.rafkos.ojkipojki.shared.protocol.event.TokensSyncEvent
import org.apache.logging.log4j.LogManager
import java.util.Timer
import java.util.TimerTask

class TokenSyncHeartbeatService(
    private val repository: ModelRepository,
    private val broadcastService: EventBroadcastService,
    private val intervalMs: Long,
) {
    private val log = LogManager.getLogger(TokenSyncHeartbeatService::class.java)
    private val timer = Timer("token-sync-heartbeat", true)

    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    val tokens = repository.findAllTokens().map { it.toState() }
                    broadcastService.broadcast(TokensSyncEvent(tokens))
                    log.debug("Broadcast token sync heartbeat (${tokens.size} tokens)")
                } catch (e: Exception) {
                    log.error("Token sync heartbeat failed: ${e.message}")
                }
            }
        }, intervalMs, intervalMs)
    }

    fun stop() = timer.cancel()
}
