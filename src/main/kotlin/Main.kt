package org.example

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.server.ServerRunner

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    ServerRunner.startServer(12002)

    ClientRunner.startClient("localhost", 12002)

}