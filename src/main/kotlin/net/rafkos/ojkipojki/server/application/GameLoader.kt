package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.server.model.SpriteBagModel
import net.rafkos.ojkipojki.server.model.TokenModel
import org.apache.logging.log4j.LogManager

object GameLoader {
    private val log = LogManager.getLogger(GameLoader::class.java)

    /** Returns false if the save file was corrupted (repo is cleared; caller may surface this to the user). */
    fun tryLoad(repo: ModelRepository): Boolean {
        if (!GamePersistence.saveFile.exists()) return true
        return try {
            val save = GamePersistence.load()
            save.spriteBags.forEach { bag ->
                repo.saveSpriteBag(SpriteBagModel().apply { apply(bag) })
            }
            save.tokens.forEach { token ->
                repo.saveToken(TokenModel().apply { apply(token) })
            }
            log.info("Loaded save: ${save.spriteBags.size} sprite bag(s), ${save.tokens.size} token(s)")
            true
        } catch (e: Exception) {
            log.warn("Save file corrupted, starting fresh session: ${e.message}")
            repo.deleteAllSpriteBags()
            repo.deleteAllTokens()
            false
        }
    }
}
