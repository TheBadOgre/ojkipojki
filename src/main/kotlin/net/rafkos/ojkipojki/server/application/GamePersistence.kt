package net.rafkos.ojkipojki.server.application

import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.Token
import java.io.*

data class GameSave(
    val spriteBags: List<SpriteBag>,
    val tokens: List<Token>,
) : Serializable {
    companion object { private const val serialVersionUID = 1L }
}

object GamePersistence {
    val saveFile = File("last_game.sav")

    fun save(repository: ModelRepository) {
        val save = GameSave(
            spriteBags = repository.findAllSpriteBags().map { it.toState() },
            tokens     = repository.findAllTokens().map { it.toState() },
        )
        ObjectOutputStream(BufferedOutputStream(FileOutputStream(saveFile))).use { it.writeObject(save) }
    }

    fun load(): GameSave {
        ObjectInputStream(BufferedInputStream(FileInputStream(saveFile))).use {
            return it.readObject() as GameSave
        }
    }
}
