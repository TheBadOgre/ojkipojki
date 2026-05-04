package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.server.ServerRunner
import net.rafkos.ojkipojki.server.application.persistence.GamePersistence
import org.apache.logging.log4j.LogManager
import java.io.File

object CliRunner {
    private val log = LogManager.getLogger(CliRunner::class.java)

    fun run(args: Array<String>) {
        val hasLoadSave = "--load-save" in args
        val hasStartFresh = "--start-fresh" in args

        if (hasLoadSave && hasStartFresh) {
            log.error("Error: --load-save and --start-fresh cannot be used together")
            System.exit(1)
            return
        }

        val saveFile: File? = when {
            hasStartFresh -> null
            hasLoadSave -> {
                val idx = args.indexOf("--load-save")
                if (idx + 1 >= args.size) {
                    log.error("Error: --load-save requires a file name argument")
                    System.exit(1)
                    return
                }
                File(GamePersistence.savesDir, args[idx + 1])
            }
            else -> GamePersistence.autoSaveFile
        }

        val portIdx = args.indexOf("--port")
        val port: Int
        if (portIdx >= 0 && portIdx + 1 < args.size) {
            val parsed = args[portIdx + 1].toIntOrNull()
            if (parsed == null) {
                log.error("Error: --port requires a valid integer argument")
                System.exit(1)
                return
            }
            port = parsed
        } else {
            port = 12001
        }

        ServerRunner.startServer(port, saveFile)
        try {
            Thread.currentThread().join()
        } catch (_: InterruptedException) {
            // normal shutdown via signal
        }
    }
}
