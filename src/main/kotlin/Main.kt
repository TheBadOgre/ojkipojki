package net.rafkos

import net.rafkos.ojkipojki.client.ClientRunner
import net.rafkos.ojkipojki.server.ServerRunner
import net.rafkos.ojkipojki.client.application.SpriteLoader
import java.io.File


fun main() {


    ServerRunner.startServer(12002)

    ClientRunner.startClient("localhost", 12002)

//    ClientRunner.startClient("localhost", 12002)

}