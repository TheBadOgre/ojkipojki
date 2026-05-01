package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.server.model.TokenModel
import org.apache.logging.log4j.LogManager
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

object GameLoader {
    private val log = LogManager.getLogger(GameLoader::class.java)

    fun tryLoad(repo: ModelRepository) {
        if (!GamePersistence.saveFile.exists()) return
        try {
            val save = GamePersistence.load()
            save.spriteBags.forEach { bag ->
                repo.saveSpriteBag(SpriteBagModel().apply { apply(bag) })
            }
            save.tokens.forEach { token ->
                repo.saveToken(TokenModel().apply { apply(token) })
            }
            log.info("Loaded save: ${save.spriteBags.size} sprite bag(s), ${save.tokens.size} token(s)")
        } catch (e: Exception) {
            log.warn("Save file corrupted, starting fresh session: ${e.message}")
            repo.deleteAllSpriteBags()
            repo.deleteAllTokens()
            SwingUtilities.invokeAndWait {
                JOptionPane.showMessageDialog(
                    null,
                    "Save file '${GamePersistence.saveFile.name}' is corrupted and could not be loaded.\nStarting a fresh session.",
                    "Corrupted Save File",
                    JOptionPane.WARNING_MESSAGE,
                )
            }
        }
    }
}
