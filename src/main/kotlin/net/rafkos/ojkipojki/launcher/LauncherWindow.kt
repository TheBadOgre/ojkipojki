package net.rafkos.ojkipojki.launcher

import net.rafkos.ojkipojki.server.application.persistence.GamePersistence
import net.rafkos.ojkipojki.shared.AppIcon
import net.rafkos.ojkipojki.shared.locale.LocaleService
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel

class LauncherWindow : JFrame(LocaleService.get("launcher.title")) {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        AppIcon.image?.let { iconImage = it }

        val controller = LauncherController(this)

        val root = JPanel()
        root.layout = BoxLayout(root, BoxLayout.Y_AXIS)
        root.add(ConnectPanel { host, port, onFailure ->
            controller.connectToServer(host, port, onFailure)
        })
        root.add(HostPanel(
            GamePersistence.listSaveFiles(),
            GamePersistence.listScenarioFiles()
        ) { port, saveFile, alsoConnect ->
            controller.hostServer(port, saveFile, alsoConnect)
        })

        contentPane.add(root)
        pack()
        minimumSize = Dimension(size.width, maxOf(size.height, 600))
        setLocationRelativeTo(null)
    }
}
