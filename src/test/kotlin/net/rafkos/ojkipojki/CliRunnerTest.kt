package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.server.application.persistence.GamePersistence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class CliRunnerTest {

    @Test
    fun `no save flags defaults to autosave file and port 12001`() {
        val result = CliRunner.parseArgs(arrayOf("--server"))
        assertSuccess(result) { (port, saveFile) ->
            assertEquals(12001, port)
            assertEquals(GamePersistence.autoSaveFile, saveFile)
        }
    }

    @Test
    fun `start-fresh yields null save file`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--start-fresh"))
        assertSuccess(result) { (_, saveFile) -> assertNull(saveFile) }
    }

    @Test
    fun `load-save resolves file under savesDir`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--load-save", "game.sav"))
        assertSuccess(result) { (_, saveFile) ->
            assertEquals(File(GamePersistence.savesDir, "game.sav"), saveFile)
        }
    }

    @Test
    fun `scenario resolves file under scenariosDir`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--scenario", "battle.sav"))
        assertSuccess(result) { (_, saveFile) ->
            assertEquals(File(GamePersistence.scenariosDir, "battle.sav"), saveFile)
        }
    }

    @Test
    fun `custom port is parsed correctly`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--start-fresh", "--port", "9999"))
        assertSuccess(result) { (port, _) -> assertEquals(9999, port) }
    }

    @Test
    fun `scenario with custom port`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--scenario", "s.sav", "--port", "8080"))
        assertSuccess(result) { (port, saveFile) ->
            assertEquals(8080, port)
            assertEquals(File(GamePersistence.scenariosDir, "s.sav"), saveFile)
        }
    }

    @Test
    fun `load-save and start-fresh together is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--load-save", "f.sav", "--start-fresh"))
        assertError(result)
    }

    @Test
    fun `load-save and scenario together is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--load-save", "f.sav", "--scenario", "s.sav"))
        assertError(result)
    }

    @Test
    fun `start-fresh and scenario together is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--start-fresh", "--scenario", "s.sav"))
        assertError(result)
    }

    @Test
    fun `load-save without argument is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--load-save"))
        assertError(result)
    }

    @Test
    fun `scenario without argument is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--scenario"))
        assertError(result)
    }

    @Test
    fun `non-integer port is an error`() {
        val result = CliRunner.parseArgs(arrayOf("--server", "--start-fresh", "--port", "notanumber"))
        assertError(result)
    }

private fun assertSuccess(result: CliRunner.ParseResult, block: (CliRunner.ParseResult.Success) -> Unit) {
        assertInstanceOf(CliRunner.ParseResult.Success::class.java, result)
        block(result as CliRunner.ParseResult.Success)
    }

    private fun assertError(result: CliRunner.ParseResult) {
        assertInstanceOf(CliRunner.ParseResult.Error::class.java, result)
    }
}
