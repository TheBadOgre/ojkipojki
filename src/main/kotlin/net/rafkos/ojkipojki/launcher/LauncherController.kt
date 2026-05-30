package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.client.protocol.AuthFailedException
import net.rafkos.ojkipojki.server.ServerRunner
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.io.File
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

class LauncherController(private val window: JFrame) {

    fun connectToServer(host: String, port: Int, password: String, onFailure: () -> Unit) {
        Thread {
            try {
                ClientRunner.startClient(host, port, password)
                SwingUtilities.invokeLater {
                    window.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    window.dispose()
                }
            } catch (e: AuthFailedException) {
                SwingUtilities.invokeLater {
                    onFailure()
                    JOptionPane.showMessageDialog(
                        window,
                        LocaleService.get("launcher.connect.error.wrongPassword.message"),
                        LocaleService.get("launcher.connect.error.wrongPassword.title"),
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    onFailure()
                    JOptionPane.showMessageDialog(
                        window,
                        LocaleService.get("launcher.connect.error.message", host, port, e.message ?: ""),
                        LocaleService.get("launcher.connect.error.title"),
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
            }
        }.apply { isDaemon = true; start() }
    }

    fun hostServer(port: Int, saveFile: File?, alsoConnect: Boolean, password: String) {
        val console = ServerConsoleWindow()
        window.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        window.dispose()
        SwingUtilities.invokeLater { console.isVisible = true }
        if (alsoConnect) {
            Thread {
                ServerRunner.startServer(port, saveFile, password.ifEmpty { null })
                try {
                    ClientRunner.startClient("localhost", port, password)
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            null,
                            LocaleService.get("launcher.connect.error.message", "localhost", port, e.message ?: ""),
                            LocaleService.get("launcher.connect.error.title"),
                            JOptionPane.ERROR_MESSAGE,
                        )
                    }
                }
            }.apply { isDaemon = true; start() }
        } else {
            Thread { ServerRunner.startServer(port, saveFile, password.ifEmpty { null }) }.apply { isDaemon = true; start() }
        }
    }
}
