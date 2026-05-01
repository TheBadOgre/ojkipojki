package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.loader.SpriteBagDirectoryLoader
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.shared.domain.Index
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand

class BoardActions(
    private val stateRepository: StateRepository,
    private val selectionState: SelectionState,
    private val transmitter: CommandTransmitter,
    private val debouncer: CommandDebouncer,
) {
    fun selectAll() = selectionState.replaceWith(stateRepository.findAllTokens().map { it.id })
    fun deselectAll() = selectionState.clear()

    fun rotate60() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, Rotation((token.rotation.degrees + 60.0) % 360.0), null, null)
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun indexUp() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, null, null, Index(token.index.value + 1))
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun indexDown() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            if (token.index.value <= 0) return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, null, null, Index(token.index.value - 1))
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun delete() {
        val ids = selectionState.selectedIds().toList()
        if (ids.isEmpty()) return
        transmitter.transmit(DeleteTokensCommand(ids))
        selectionState.clear()
    }

    fun refreshBags() {
        val bags = SpriteBagDirectoryLoader.loadAll()
        transmitter.transmit(UploadSpriteBagsCommand(bags))
    }
}
