package net.rafkos.ojkipojki.client.view.panel

import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class StatusBarPanel(serverIp: String) : JPanel(BorderLayout()) {
    init {
        border = BorderFactory.createEtchedBorder()
        add(JLabel("  Connected to: $serverIp"), BorderLayout.WEST)
    }
}
