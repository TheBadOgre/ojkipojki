package net.rafkos.ojkipojki.shared.protocol.command

data class AuthCommand(val password: String) : Command {
    companion object {
        private const val serialVersionUID = 1L
    }
}
