package org.example

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.server.ServerRunner


fun main() {
    ServerRunner.startServer(12002)

    ClientRunner.startClient("localhost", 12002)

    ClientRunner.startClient("localhost", 12002)

}