package net.rafkos.ojkipojki.shared

import java.io.File

object AppDirs {
    private val appDir: String? = System.getProperty("app.dir")
    private val installRoot: File? = if (appDir != null) File(appDir).parentFile else null

    val root: File = File(appDir ?: ".")

    val spritesRoot: File = installRoot?.resolve("sprites") ?: root.resolve("sprites")

    val dataRoot: File = if (installRoot != null) {
        val os = System.getProperty("os.name", "").lowercase()
        val home = File(System.getProperty("user.home", "."))
        when {
            os.contains("mac") -> File(home, "Library/Application Support/ojkipojki")
            os.contains("win") -> installRoot
            else               -> File(home, ".local/share/ojkipojki")
        }
    } else {
        File(".")
    }

    fun resolve(path: String): File = root.resolve(path)
    fun resolveData(path: String): File = dataRoot.resolve(path)
}
