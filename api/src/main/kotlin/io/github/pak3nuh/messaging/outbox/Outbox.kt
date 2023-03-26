package io.github.pak3nuh.messaging.outbox

interface Outbox : AutoCloseable {
    fun submit(entry: Entry)
    fun start()
}