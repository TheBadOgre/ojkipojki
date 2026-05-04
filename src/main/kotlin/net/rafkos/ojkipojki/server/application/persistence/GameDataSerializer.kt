package net.rafkos.ojkipojki.server.application.persistence

import net.rafkos.ojkipojki.server.application.ModelRepository

interface GameDataSerializer<G : GameSave> {
    fun serialize(repository: ModelRepository): G
}
