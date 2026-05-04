package net.rafkos.ojkipojki.support

import java.net.ServerSocket
import java.net.Socket

fun socketPair(): Pair<Socket, Socket> {
    val server = ServerSocket(0)
    val client = Socket("127.0.0.1", server.localPort)
    val accepted = server.accept()
    server.close()
    return client to accepted
}

fun await(timeoutMs: Long = 1000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    require(condition()) { "Timed out after ${timeoutMs}ms" }
}
