package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.protocol.command.CommandTransmitter

interface SessionLifecycleListener {
    fun onSessionReady(transmitter: CommandTransmitter)
    fun onSessionClosed()
}
