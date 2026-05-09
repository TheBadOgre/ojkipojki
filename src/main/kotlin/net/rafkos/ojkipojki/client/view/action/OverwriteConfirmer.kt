package net.rafkos.ojkipojki.client.view.action

import net.rafkos.ojkipojki.shared.locale.LocaleService
import javax.swing.JOptionPane

interface OverwriteConfirmer {
    fun confirmOverwrite(conflictingIds: List<String>): Boolean
}

class SwingOverwriteConfirmer : OverwriteConfirmer {
    override fun confirmOverwrite(conflictingIds: List<String>): Boolean {
        val message = if (conflictingIds.size == 1) {
            LocaleService.get("spritebag.overwriteMessage", conflictingIds[0])
        } else {
            LocaleService.get("spritebag.overwriteAllMessage", conflictingIds.joinToString("\n"))
        }
        val title = if (conflictingIds.size == 1)
            LocaleService.get("spritebag.overwriteTitle")
        else
            LocaleService.get("spritebag.overwriteAllTitle")
        val yes = LocaleService.get("dialog.yes")
        val no = LocaleService.get("dialog.no")
        return JOptionPane.showOptionDialog(
            null, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, arrayOf(yes, no), yes
        ) == 0
    }
}
