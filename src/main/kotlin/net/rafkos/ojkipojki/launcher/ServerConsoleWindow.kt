package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.shared.AppIcon
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.Color
import java.awt.Font
import java.io.OutputStream
import java.io.PrintStream
import javax.swing.*

class ServerConsoleWindow : JFrame(LocaleService.get("server.console.title")) {
    private val textArea = JTextArea().apply {
        isEditable = false
        background = Color(20, 20, 20)
        foreground = Color(204, 204, 204)
        font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        caretColor = Color(204, 204, 204)
        margin = java.awt.Insets(4, 6, 4, 6)
        lineWrap = true
        wrapStyleWord = false
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        AppIcon.image?.let { iconImage = it }
        setSize(700, 450)
        add(JScrollPane(textArea).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        })
        setLocationRelativeTo(null)
        redirectOutput()
    }

    private fun redirectOutput() {
        val originalOut = System.out
        val originalErr = System.err
        val tee = PrintStream(TeeOutputStream(originalOut, TextAreaOutputStream()), true)
        System.setOut(tee)
        System.setErr(tee)

        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosed(e: java.awt.event.WindowEvent) {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
        })
    }

    private inner class TextAreaOutputStream : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            val text = String(b, off, len)
            SwingUtilities.invokeLater {
                textArea.append(text)
                val lineCount = textArea.lineCount
                if (lineCount > 500) {
                    try {
                        val endOffset = textArea.getLineEndOffset(lineCount - 501)
                        textArea.document.remove(0, endOffset)
                    } catch (_: Exception) {}
                }
                textArea.caretPosition = textArea.document.length
            }
        }
    }

    private class TeeOutputStream(
        private val first: OutputStream,
        private val second: OutputStream,
    ) : OutputStream() {
        override fun write(b: Int) { first.write(b); second.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { first.write(b, off, len); second.write(b, off, len) }
        override fun flush() { first.flush(); second.flush() }
    }
}
