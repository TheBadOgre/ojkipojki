package net.rafkos.ojkipojki.client.view.panel

import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class StatusBarPanel(serverIp: String) : JPanel(BorderLayout()) {
    private val clientCountLabel = JLabel("")

    init {
        border = BorderFactory.createEtchedBorder()
        add(JLabel("  " + LocaleService.get("status.connectedTo", serverIp)), BorderLayout.WEST)
        add(clientCountLabel, BorderLayout.EAST)
    }

    fun updateClientCount(count: Int) {
        clientCountLabel.text = LocaleService.get("status.players", count) + "  "
    }
}
