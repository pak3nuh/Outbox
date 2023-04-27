package io.github.pak3nuh.messaging.outbox

/**
 * Outbox facade interface
 */
interface Outbox : AutoCloseable {
    /**
     * Submits an entry to be sent
     */
    fun submit(entry: Entry)

    /**
     * Processes every entry it has that has not yet marked as processed.
     */
    fun processEntries()
}