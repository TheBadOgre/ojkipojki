package net.rafkos.ojkipojki.shared.protocol

interface Handler<A> {
    fun handle(action: A)
}
