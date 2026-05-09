package net.rafkos.ojkipojki

import net.rafkos.ojkipojki.launcher.LauncherWindow
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    if ("--server" in args) {
        CliRunner.run(args)
    } else {
        System.setProperty("sun.java2d.opengl", "true")
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        SwingUtilities.invokeLater { LauncherWindow().isVisible = true }
    }
}
