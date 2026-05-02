package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.launcher.LauncherWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    SwingUtilities.invokeLater { LauncherWindow().isVisible = true }
}