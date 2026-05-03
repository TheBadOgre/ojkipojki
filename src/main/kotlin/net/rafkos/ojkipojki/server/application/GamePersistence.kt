package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.Token
import java.io.*
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class GameSave(
    val spriteBags: List<SpriteBag>,
    val tokens: List<Token>,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

object GamePersistence {
    val savesDir = File("saves")
    val autoSaveFile get() = File(savesDir, "autosave.sav")

    fun save(repository: ModelRepository, file: File = autoSaveFile) {
        savesDir.mkdirs()
        val save = GameSave(
            spriteBags = repository.findAllSpriteBags().map { it.toState() },
            tokens     = repository.findAllTokens().map { it.toState() },
        )
        ObjectOutputStream(BufferedOutputStream(FileOutputStream(file))).use { it.writeObject(save) }
    }

    fun load(file: File): GameSave {
        ObjectInputStream(BufferedInputStream(FileInputStream(file))).use {
            return it.readObject() as GameSave
        }
    }

    fun listSaveFiles(): List<File> =
        (savesDir.listFiles { f -> f.extension == "sav" } ?: emptyArray<File>())
            .sortedByDescending { it.lastModified() }

    fun timestampedAutoSaveFile(): File {
        val stamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        return File(savesDir, "autosave_${stamp}_UTC.sav")
    }
}
