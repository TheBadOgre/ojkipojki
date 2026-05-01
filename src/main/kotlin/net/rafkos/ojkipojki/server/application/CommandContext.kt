package net.rafkos.ojkipojki.server.application

object CommandContext {
    private val local = ThreadLocal<String?>()
    var clientId: String?
        get() = local.get()
        set(value) { local.set(value) }
}
