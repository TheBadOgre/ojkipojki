package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.server.ServerRunner
import net.rafkos.ojkipojki.server.application.persistence.GamePersistence
import org.apache.logging.log4j.LogManager
import java.io.File

object CliRunner {
    private val log = LogManager.getLogger(CliRunner::class.java)

    sealed class ParseResult {
        data class Success(val port: Int, val saveFile: File?) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    fun parseArgs(args: Array<String>): ParseResult {
        val hasLoadSave = "--load-save" in args
        val hasStartFresh = "--start-fresh" in args
        val hasScenario = "--scenario" in args

        val activeFlags = listOfNotNull(
            if (hasLoadSave) "--load-save" else null,
            if (hasStartFresh) "--start-fresh" else null,
            if (hasScenario) "--scenario" else null,
        )
        if (activeFlags.size > 1) {
            return ParseResult.Error("${activeFlags[0]} and ${activeFlags[1]} cannot be used together")
        }

        val saveFile: File? = when {
            hasStartFresh -> null
            hasLoadSave -> {
                val idx = args.indexOf("--load-save")
                if (idx + 1 >= args.size) {
                    return ParseResult.Error("--load-save requires a file name argument")
                }
                File(GamePersistence.savesDir, args[idx + 1])
            }
            hasScenario -> {
                val idx = args.indexOf("--scenario")
                if (idx + 1 >= args.size) {
                    return ParseResult.Error("--scenario requires a file name argument")
                }
                File(GamePersistence.scenariosDir, args[idx + 1])
            }
            else -> GamePersistence.autoSaveFile
        }

        val portIdx = args.indexOf("--port")
        val port: Int = if (portIdx >= 0 && portIdx + 1 < args.size) {
            args[portIdx + 1].toIntOrNull()
                ?: return ParseResult.Error("--port requires a valid integer argument")
        } else {
            12001
        }

        return ParseResult.Success(port, saveFile)
    }

    fun run(args: Array<String>) {
        when (val result = parseArgs(args)) {
            is ParseResult.Error -> {
                log.error("Error: ${result.message}")
                System.exit(1)
                return
            }
            is ParseResult.Success -> {
                ServerRunner.startServer(result.port, result.saveFile)
                try {
                    Thread.currentThread().join()
                } catch (_: InterruptedException) {
                    // normal shutdown via signal
                }
            }
        }
    }
}
