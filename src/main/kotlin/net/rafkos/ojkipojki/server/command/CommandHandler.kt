package net.rafkos.ojkipojki.server.command

interface CommandHandler {
    fun handle(command: Command)
}