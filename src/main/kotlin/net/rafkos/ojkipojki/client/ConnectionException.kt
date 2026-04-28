package net.rafkos.ojkipojki.client

sealed class ConnectionException : Exception() {
    class AlreadyConnectedException : ConnectionException()
    class UnableToConnectException : ConnectionException()
}