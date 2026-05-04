package net.rafkos.ojkipojki.shared

import java.io.File

object AppDirs {
    val root: File = File(System.getProperty("app.dir", "."))

    val dataRoot: File = if (System.getProperty("app.dir") != null) {
        val os = System.getProperty("os.name", "").lowercase()
        val home = File(System.getProperty("user.home", "."))
        when {
            os.contains("mac") -> File(home, "Library/Application Support/ojkipojki")
            os.contains("win") -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming", "ojkipojki")
            else               -> File(home, ".local/share/ojkipojki")
        }
    } else {
        File(".")
    }

    fun resolve(path: String): File = root.resolve(path)
    fun resolveData(path: String): File = dataRoot.resolve(path)
}
