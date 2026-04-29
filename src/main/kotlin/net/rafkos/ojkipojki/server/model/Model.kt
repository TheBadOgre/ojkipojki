package net.rafkos.ojkipojki.server.model

import net.rafkos.ojkipojki.shared.domain.DomainModel

class Model {
    private val models = mutableMapOf<String, DomainModel>()

    fun addModel(model: DomainModel) {
        models[model.id] = model
    }

    fun getModel(id: String): DomainModel? = models[id]

    fun getAllModels(): List<DomainModel> = models.values.toList()

    fun updateModel(model: DomainModel) {
        models[model.id] = model
    }
}
