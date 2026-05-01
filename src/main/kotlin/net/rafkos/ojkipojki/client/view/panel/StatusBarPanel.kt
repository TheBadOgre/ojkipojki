package net.rafkos.ojkipojki.client.view.panel

import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class StatusBarPanel(serverIp: String) : JPanel(BorderLayout()) {
    private val clientCountLabel = JLabel("")

    init {
        border = BorderFactory.createEtchedBorder()
        add(JLabel("  Connected to: $serverIp"), BorderLayout.WEST)
        add(clientCountLabel, BorderLayout.EAST)
    }

    fun updateClientCount(count: Int) {
        clientCountLabel.text = "Players: $count  "
    }
}
