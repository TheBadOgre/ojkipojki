package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.launcher.LauncherWindow
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater { LauncherWindow().isVisible = true }
}