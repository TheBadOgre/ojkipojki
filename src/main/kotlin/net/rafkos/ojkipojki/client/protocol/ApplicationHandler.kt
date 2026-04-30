package net.rafkos.ojkipojki.client.protocol

import net.rafkos.ojkipojki.client.command.CommandTransmitter

interface ApplicationHandler {
    fun onSessionReady(transmitter: CommandTransmitter)
    fun onSessionClosed()
}
