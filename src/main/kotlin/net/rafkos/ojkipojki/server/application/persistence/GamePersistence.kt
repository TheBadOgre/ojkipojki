package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.server.application.ModelRepository
import net.rafkos.ojkipojki.shared.AppDirs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object GamePersistence {
    val savesDir = AppDirs.resolveData("saves")
    val autoSaveFile get() = File(savesDir, "autosave.sav")

    private val serializer: GameDataSerializer<*> = GameDataV1Serializer()

    fun save(repository: ModelRepository, file: File = autoSaveFile) {
        savesDir.mkdirs()
        val save = serializer.serialize(repository)
        ObjectOutputStream(GZIPOutputStream(FileOutputStream(file))).use { it.writeObject(save) }
    }

    fun load(file: File): GameData {
        val gameSave = ObjectInputStream(GZIPInputStream(FileInputStream(file))).use { it.readObject() }
        return when (gameSave) {
            is GameDataV1 -> GameDataV1Deserializer().deserialize(gameSave)
            else -> throw IllegalArgumentException("Unknown save format: ${gameSave::class.java.name}")
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
