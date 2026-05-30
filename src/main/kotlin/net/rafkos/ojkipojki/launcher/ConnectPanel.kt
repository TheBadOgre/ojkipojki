package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class ConnectPanel(
    private val onConnect: (host: String, port: Int, onFailure: () -> Unit) -> Unit
) : JPanel(GridBagLayout()) {
    override fun getMaximumSize() = java.awt.Dimension(Int.MAX_VALUE, preferredSize.height)
    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 8, 4, 8),
            BorderFactory.createTitledBorder(LocaleService.get("launcher.connect.title"))
        )

        val hostLabel = JLabel(LocaleService.get("launcher.connect.host"))
        val portLabel = JLabel(LocaleService.get("launcher.connect.port"))
        val hostField = JTextField("127.0.0.1", 12)
        val portField = JTextField("12001", 6)
        val connectButton = JButton(LocaleService.get("launcher.connect.button"))

        val gbc = GridBagConstraints().apply { insets = Insets(4, 6, 4, 6) }

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST
        add(hostLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 0
        add(portLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        add(hostField, gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.0
        add(portField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        add(connectButton, gbc)

        connectButton.addActionListener {
            val host = hostField.text.trim()
            val port = portField.text.trim().toIntOrNull() ?: return@addActionListener
            connectButton.isEnabled = false
            onConnect(host, port) { connectButton.isEnabled = true }
        }
    }
}
