package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.server.ServerRunner
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class LauncherWindow : JFrame("Ojkipojki") {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE

        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.add(buildConnectPanel())
        root.add(buildHostPanel())

        contentPane.add(root)
        pack()
        setLocationRelativeTo(null)
    }

    private fun buildConnectPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(8, 8, 4, 8),
            BorderFactory.createEtchedBorder()
        )

        val hostLabel = JLabel("host")
        val portLabel = JLabel("port")
        val hostField = JTextField("127.0.0.1", 12)
        val portField = JTextField("12001", 6)
        val connectButton = JButton("connect to server")

        val gbc = GridBagConstraints().apply { insets = Insets(4, 6, 4, 6) }

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST
        panel.add(hostLabel, gbc)
        gbc.gridx = 1; gbc.gridy = 0
        panel.add(portLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(hostField, gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.0
        panel.add(portField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(connectButton, gbc)

        connectButton.addActionListener {
            val host = hostField.text.trim()
            val port = portField.text.trim().toIntOrNull() ?: return@addActionListener
            connectButton.isEnabled = false
            Thread {
                try {
                    ClientRunner.startClient(host, port)
                    SwingUtilities.invokeLater {
                        defaultCloseOperation = DISPOSE_ON_CLOSE
                        dispose()
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        connectButton.isEnabled = true
                        JOptionPane.showMessageDialog(
                            this@LauncherWindow,
                            "Cannot connect to $host:$port\n${e.message}",
                            "Connection Failed",
                            JOptionPane.ERROR_MESSAGE,
                        )
                    }
                }
            }.apply { isDaemon = true; start() }
        }

        return panel
    }

    private fun buildHostPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(4, 8, 8, 8),
            BorderFactory.createEtchedBorder()
        )

        val portLabel = JLabel("port")
        val portField = JTextField("12001")
        val hostButton = JButton("host server")

        val gbc = GridBagConstraints().apply { insets = Insets(4, 6, 4, 6) }

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST
        panel.add(portLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(portField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(hostButton, gbc)

        hostButton.addActionListener {
            val port = portField.text.trim().toIntOrNull() ?: return@addActionListener
            val console = ServerConsoleWindow()
            defaultCloseOperation = DISPOSE_ON_CLOSE
            dispose()
            SwingUtilities.invokeLater { console.isVisible = true }
            Thread { ServerRunner.startServer(port) }.apply { isDaemon = true; start() }
        }

        return panel
    }
}
