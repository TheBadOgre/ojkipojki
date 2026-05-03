package net.rafkos.ojkipojki.client.view

import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import javax.swing.BorderFactory
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.WindowConstants

class InitializationProgressDialog(owner: Frame) : JDialog(owner, false) {

    private val messageLabel = JLabel(" ")
    private val progressBar = JProgressBar(0, 100).apply {
        isStringPainted = true
    }

    init {
        title = LocaleService.get("main.window.title")
        isResizable = false
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

        val panel = JPanel(BorderLayout(0, 8))
        panel.border = BorderFactory.createEmptyBorder(16, 24, 16, 24)
        panel.preferredSize = Dimension(340, 90)
        panel.add(messageLabel, BorderLayout.NORTH)
        panel.add(progressBar, BorderLayout.CENTER)

        contentPane = panel
        pack()
        setLocationRelativeTo(owner)
    }

    fun update(messageKey: String, progress: Double) {
        messageLabel.text = LocaleService.get(messageKey)
        val pct = (progress * 100).toInt().coerceIn(0, 100)
        progressBar.value = pct
        progressBar.string = "$pct%"
    }
}
