package net.rafkos.ojkipojki.shared

interface Handler<A> {
    fun handle(action: A)
}