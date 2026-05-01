package net.rafkos.ojkipojki.server.application

import org.apache.logging.log4j.LogManager
import java.util.Timer
import java.util.TimerTask

class AutoSaveService(
    private val repository: ModelRepository,
    private val intervalMs: Long,
) {
    private val log = LogManager.getLogger(AutoSaveService::class.java)
    private val timer = Timer("auto-save", true)

    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    GamePersistence.save(repository)
                    log.info("Auto-saved game state")
                } catch (e: Exception) {
                    log.error("Auto-save failed: ${e.message}")
                }
            }
        }, intervalMs, intervalMs)
    }

    fun stop() = timer.cancel()
}
