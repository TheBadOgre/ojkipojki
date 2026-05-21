package net.rafkos.ojkipojki.client.view.action

import kotlinx.coroutines.runBlocking
import net.rafkos.ojkipojki.client.ClientContext
import net.rafkos.ojkipojki.client.application.StateRepository
import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter
import net.rafkos.ojkipojki.client.view.state.SelectionState
import net.rafkos.ojkipojki.client.view.state.TokenAnimator
import net.rafkos.ojkipojki.shared.domain.SpriteBag
import net.rafkos.ojkipojki.shared.domain.Index
import net.rafkos.ojkipojki.shared.domain.Position
import net.rafkos.ojkipojki.shared.domain.Rotation
import net.rafkos.ojkipojki.shared.protocol.command.DeleteTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.LockTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.MoveTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.ShuffleTokensCommand
import net.rafkos.ojkipojki.shared.protocol.command.UploadSpriteBagsCommand
import kotlin.math.ceil
import kotlin.math.sqrt

class BoardActions(
    private val stateRepository: StateRepository,
    private val selectionState: SelectionState,
    private val transmitter: CommandTransmitter,
    private val debouncer: CommandDebouncer,
    private val tokenAnimator: TokenAnimator,
    private val confirmer: OverwriteConfirmer = SwingOverwriteConfirmer(),
    private val onLocalRegistryReloaded: () -> Unit = {},
) {
    fun selectAll() = selectionState.replaceWith(stateRepository.findAllTokens().map { it.id })
    fun deselectAll() = selectionState.clear()

    fun rotate60() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            if (token.locked) return@mapNotNull null
            val newRotation = Rotation((token.rotation.degrees + 60.0) % 360.0)
            tokenAnimator.updateDragRotationIfPresent(id, newRotation.degrees)
            MoveTokensCommand.TokenIdAndPosition(id, null, newRotation, null, null)
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun indexUp() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            if (token.locked) return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, null, null, Index(token.index.value + 1))
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun indexDown() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            if (token.locked) return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, null, null, Index(token.index.value - 1))
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun bringToFront() {
        val selected = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.index.value }
        if (selected.isEmpty()) return
        val maxIndex = stateRepository.findAllTokens().maxOfOrNull { it.index.value } ?: 0
        val adjustments = selected.mapIndexed { i, token ->
            MoveTokensCommand.TokenIdAndPosition(token.id, null, null, null, Index(maxIndex + 1 + i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun bringToBack() {
        val selected = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.index.value }
        if (selected.isEmpty()) return
        val minIndex = stateRepository.findAllTokens().minOfOrNull { it.index.value } ?: 0
        val adjustments = selected.mapIndexed { i, token ->
            MoveTokensCommand.TokenIdAndPosition(token.id, null, null, null, Index(minIndex - selected.size + i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun delete() {
        val ids = selectionState.selectedIds()
            .filter { id -> stateRepository.findTokenById(id)?.locked != true }
            .toList()
        if (ids.isEmpty()) return
        transmitter.transmit(DeleteTokensCommand(ids))
        selectionState.clear()
    }

    fun refreshBags() {
        runBlocking { ClientContext.localSpriteBagRegistry.reload() }
        onLocalRegistryReloaded()
        val stateIds = stateRepository.findAllSpriteBags().map { it.id }.toSet()
        val toResend = ClientContext.localSpriteBagRegistry.bags().filter { it.id in stateIds }
        if (toResend.isNotEmpty()) transmitter.transmit(UploadSpriteBagsCommand(toResend))
    }

    fun uploadLocalBag(bag: SpriteBag) {
        val hasConflict = stateRepository.findSpriteBagById(bag.id) != null
        if (hasConflict && !confirmer.confirmOverwrite(listOf(bag.id.id))) return
        transmitter.transmit(UploadSpriteBagsCommand(listOf(bag)))
    }

    fun uploadAllLocal() {
        val stateIds = stateRepository.findAllSpriteBags().map { it.id }.toSet()
        val allLocal = ClientContext.localSpriteBagRegistry.bags()
        val conflicts = allLocal.filter { it.id in stateIds }
        val nonConflicts = allLocal.filter { it.id !in stateIds }
        if (conflicts.isNotEmpty() && !confirmer.confirmOverwrite(conflicts.map { it.id.id })) {
            if (nonConflicts.isNotEmpty()) transmitter.transmit(UploadSpriteBagsCommand(nonConflicts))
            return
        }
        val toUpload = conflicts + nonConflicts
        if (toUpload.isNotEmpty()) transmitter.transmit(UploadSpriteBagsCommand(toUpload))
    }

    fun flip() {
        val adjustments = selectionState.selectedIds().mapNotNull { id ->
            val token = stateRepository.findTokenById(id) ?: return@mapNotNull null
            if (token.locked) return@mapNotNull null
            MoveTokensCommand.TokenIdAndPosition(id, null, null, !token.flipped, null)
        }
        if (adjustments.isNotEmpty()) transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun stack() {
        val tokens = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.index.value }
        if (tokens.isEmpty()) return

        val cx = tokens.map { it.position.x }.average().toInt()
        val cy = tokens.map { it.position.y }.average().toInt()

        val adjustments = tokens.mapIndexed { i, token ->
            MoveTokensCommand.TokenIdAndPosition(token.id, Position(cx, cy + i * 3), null, null, Index(i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun shuffle() {
        val ids = selectionState.selectedIds()
            .filter { id -> stateRepository.findTokenById(id)?.locked != true }
            .toList()
        if (ids.size < 2) return
        transmitter.transmit(ShuffleTokensCommand(ids))
    }

    fun spreadHorizontal() {
        val tokens = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.position.x }
        if (tokens.size < 2) return

        val cy = tokens.map { it.position.y }.average().toInt()
        val gap = 100
        val startX = tokens.map { it.position.x }.average().toInt() - gap * (tokens.size - 1) / 2

        val adjustments = tokens.mapIndexed { i, token ->
            MoveTokensCommand.TokenIdAndPosition(token.id, Position(startX + i * gap, cy), null, null, Index(i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun spreadVertical() {
        val tokens = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.position.y }
        if (tokens.size < 2) return

        val cx = tokens.map { it.position.x }.average().toInt()
        val gap = 100
        val startY = tokens.map { it.position.y }.average().toInt() - gap * (tokens.size - 1) / 2

        val adjustments = tokens.mapIndexed { i, token ->
            MoveTokensCommand.TokenIdAndPosition(token.id, Position(cx, startY + i * gap), null, null, Index(i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun arrangeGrid() {
        val tokens = selectionState.selectedIds()
            .mapNotNull { stateRepository.findTokenById(it) }
            .filter { !it.locked }
            .sortedBy { it.index.value }
        if (tokens.isEmpty()) return

        val cols = ceil(sqrt(tokens.size.toDouble())).toInt()
        val cx = tokens.map { it.position.x }.average().toInt()
        val cy = tokens.map { it.position.y }.average().toInt()
        val gap = 90

        val rows = ceil(tokens.size.toDouble() / cols).toInt()
        val startX = cx - (cols - 1) * gap / 2
        val startY = cy - (rows - 1) * gap / 2

        val adjustments = tokens.mapIndexed { i, token ->
            val col = i % cols
            val row = i / cols
            MoveTokensCommand.TokenIdAndPosition(token.id, Position(startX + col * gap, startY + row * gap), null, null, Index(i))
        }
        transmitter.transmit(MoveTokensCommand(adjustments))
    }

    fun isAllSelectedLocked(): Boolean {
        val ids = selectionState.selectedIds()
        if (ids.isEmpty()) return false
        return ids.all { id -> stateRepository.findTokenById(id)?.locked == true }
    }

    fun toggleLock() {
        val ids = selectionState.selectedIds().toList()
        if (ids.isEmpty()) return
        val lockValue = !isAllSelectedLocked()
        transmitter.transmit(LockTokensCommand(ids, lockValue))
    }

    fun hasSelection(): Boolean = selectionState.selectedIds().isNotEmpty()

    fun hasUnlockedSelected(): Boolean =
        selectionState.selectedIds().any { id -> stateRepository.findTokenById(id)?.locked != true }

    fun hasAtLeast2UnlockedSelected(): Boolean =
        selectionState.selectedIds().count { id -> stateRepository.findTokenById(id)?.locked != true } >= 2
}
