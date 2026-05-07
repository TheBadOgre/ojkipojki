package net.rafkos.ojkipojki.client.view.action

import javax.swing.JOptionPane

interface OverwriteConfirmer {
    fun confirmOverwrite(conflictingIds: List<String>): Boolean
}

class SwingOverwriteConfirmer : OverwriteConfirmer {
    override fun confirmOverwrite(conflictingIds: List<String>): Boolean {
        val message = if (conflictingIds.size == 1) {
            net.rafkos.ojkipojki.shared.locale.LocaleService.get(
                "spritebag.overwriteMessage", conflictingIds[0])
        } else {
            net.rafkos.ojkipojki.shared.locale.LocaleService.get(
                "spritebag.overwriteAllMessage", conflictingIds.joinToString("\n"))
        }
        val title = if (conflictingIds.size == 1)
            net.rafkos.ojkipojki.shared.locale.LocaleService.get("spritebag.overwriteTitle")
        else
            net.rafkos.ojkipojki.shared.locale.LocaleService.get("spritebag.overwriteAllTitle")
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
    }
}
