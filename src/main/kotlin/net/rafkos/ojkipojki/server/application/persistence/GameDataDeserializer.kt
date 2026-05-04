package net.rafkos.ojkipojki.server.application.persistence

interface GameDataDeserializer<G : GameSave> {
    fun deserialize(gameSave: G): GameData
}
