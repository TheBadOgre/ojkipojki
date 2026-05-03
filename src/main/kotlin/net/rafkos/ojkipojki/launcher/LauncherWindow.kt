package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.server.ServerRunner
import net.rafkos.ojkipojki.server.application.GamePersistence
import net.rafkos.ojkipojki.shared.AppIcon
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.*

class LauncherWindow : JFrame(LocaleService.get("launcher.title")) {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        AppIcon.image?.let { iconImage = it }

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
            BorderFactory.createTitledBorder(LocaleService.get("launcher.connect.title"))
        )

        val hostLabel = JLabel(LocaleService.get("launcher.connect.host"))
        val portLabel = JLabel(LocaleService.get("launcher.connect.port"))
        val hostField = JTextField("127.0.0.1", 12)
        val portField = JTextField("12001", 6)
        val connectButton = JButton(LocaleService.get("launcher.connect.button"))

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
                            LocaleService.get("launcher.connect.error.message", host, port, e.message ?: ""),
                            LocaleService.get("launcher.connect.error.title"),
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
            BorderFactory.createTitledBorder(LocaleService.get("launcher.host.title"))
        )

        val saveFiles: List<File> = GamePersistence.listSaveFiles()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        val listModel = DefaultListModel<String>()
        saveFiles.forEach { f ->
            listModel.addElement("%-30s %s".format(f.name, dateFormat.format(Date(f.lastModified()))))
        }
        val savesList = JList(listModel)
        savesList.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        savesList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        var pressedAlreadySelected = false
        val deselectionListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val idx = savesList.locationToIndex(e.point)
                pressedAlreadySelected = idx >= 0 && savesList.isSelectedIndex(idx)
            }
            override fun mouseClicked(e: MouseEvent) {
                val idx = savesList.locationToIndex(e.point)
                if (idx >= 0 && pressedAlreadySelected) {
                    savesList.clearSelection()
                }
            }
        }
        // Must be inserted before the L&F listener so mousePressed fires before selection changes
        val existingListeners = savesList.mouseListeners.toList()
        existingListeners.forEach { savesList.removeMouseListener(it) }
        savesList.addMouseListener(deselectionListener)
        existingListeners.forEach { savesList.addMouseListener(it) }
        val savesScroll = JScrollPane(savesList)
        savesScroll.preferredSize = Dimension(400, 100)

        val savedGameLabel = JLabel(LocaleService.get("launcher.host.savedGame"))
        val hintLabel = JLabel(LocaleService.get("launcher.host.saveHint"))
        hintLabel.font = hintLabel.font.deriveFont(Font.ITALIC, 11f)
        hintLabel.foreground = java.awt.Color(100, 100, 100)

        val portLabel = JLabel(LocaleService.get("launcher.host.port"))
        val portField = JTextField("12001")
        val hostButton = JButton(LocaleService.get("launcher.host.button"))

        val gbc = GridBagConstraints().apply { insets = Insets(4, 6, 4, 6) }

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(savedGameLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0
        panel.add(savesScroll, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.weighty = 0.0
        panel.add(hintLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(portLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(portField, gbc)

        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(hostButton, gbc)

        hostButton.addActionListener {
            val port = portField.text.trim().toIntOrNull() ?: return@addActionListener
            val selectedIdx = savesList.selectedIndex
            val selectedFile = if (selectedIdx >= 0 && selectedIdx < saveFiles.size) saveFiles[selectedIdx] else null
            val console = ServerConsoleWindow()
            defaultCloseOperation = DISPOSE_ON_CLOSE
            dispose()
            SwingUtilities.invokeLater { console.isVisible = true }
            Thread { ServerRunner.startServer(port, selectedFile) }.apply { isDaemon = true; start() }
        }

        return panel
    }
}
