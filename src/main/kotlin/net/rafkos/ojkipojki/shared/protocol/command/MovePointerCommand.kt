package net.rafkos.ojkipojki.shared.protocol.command

data class MovePointerCommand(val x: Int, val y: Int) : Command {
    companion object { private const val serialVersionUID = 1L }
}
