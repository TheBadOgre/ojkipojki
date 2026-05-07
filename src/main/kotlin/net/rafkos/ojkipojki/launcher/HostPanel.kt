package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.Color
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

class HostPanel(
    saveFiles: List<File>,
    scenarioFiles: List<File>,
    private val onHost: (port: Int, saveFile: File?, alsoConnect: Boolean) -> Unit
) : JPanel(GridBagLayout()) {
    private val allFiles: List<File> = saveFiles + scenarioFiles

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(4, 8, 8, 8),
            BorderFactory.createTitledBorder(LocaleService.get("launcher.host.title"))
        )

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
        val listModel = DefaultListModel<String>()
        saveFiles.forEach { f ->
            listModel.addElement("%-30s %s".format(f.name, dateFormat.format(Date(f.lastModified()))))
        }
        scenarioFiles.forEach { f ->
            listModel.addElement("%-30s [scenario]".format(f.name))
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
                if (idx >= 0 && pressedAlreadySelected) savesList.clearSelection()
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
        hintLabel.foreground = Color(100, 100, 100)

        val portLabel = JLabel(LocaleService.get("launcher.host.port"))
        val portField = JTextField("12001")
        val hostButton = JButton(LocaleService.get("launcher.host.button"))
        val alsoConnectCheckbox = JCheckBox(LocaleService.get("launcher.host.alsoConnect"), true)

        val gbc = GridBagConstraints().apply { insets = Insets(4, 6, 4, 6) }

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        add(savedGameLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0
        add(savesScroll, gbc)

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.weighty = 0.0
        add(hintLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        add(portLabel, gbc)

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        add(portField, gbc)

        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL
        add(hostButton, gbc)

        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.HORIZONTAL
        add(alsoConnectCheckbox, gbc)

        hostButton.addActionListener {
            val port = portField.text.trim().toIntOrNull() ?: return@addActionListener
            val selectedIdx = savesList.selectedIndex
            val selectedFile = if (selectedIdx >= 0 && selectedIdx < allFiles.size) allFiles[selectedIdx] else null
            onHost(port, selectedFile, alsoConnectCheckbox.isSelected)
        }
    }
}
