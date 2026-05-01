package net.rafkos.ojkipojki.client.view.state

import net.rafkos.ojkipojki.shared.domain.Token
import net.rafkos.ojkipojki.shared.domain.TokenId

class SelectionState {
    private val ids = mutableSetOf<TokenId>()
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(l: () -> Unit) { listeners.add(l) }
    private fun notifyListeners() { listeners.forEach { it() } }

    fun selectedIds(): Set<TokenId> = ids.toSet()
    fun contains(id: TokenId) = id in ids

    fun select(id: TokenId) { ids.add(id); notifyListeners() }
    fun toggle(id: TokenId) { if (!ids.remove(id)) ids.add(id); notifyListeners() }
    fun replaceWith(incoming: Collection<TokenId>) { ids.clear(); ids.addAll(incoming); notifyListeners() }
    fun addAll(incoming: Collection<TokenId>) { ids.addAll(incoming); notifyListeners() }
    fun clear() { ids.clear(); notifyListeners() }

    fun pruneAgainst(current: Collection<Token>) {
        val live = current.map { it.id }.toSet()
        if (ids.removeAll { it !in live }) notifyListeners()
    }
}
