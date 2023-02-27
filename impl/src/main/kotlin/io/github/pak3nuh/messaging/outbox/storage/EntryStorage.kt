package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.Entry
import java.time.Instant

interface EntryStorage: AutoCloseable {
    fun persist(entry: Entry)

    fun getBatch(): Sequence<StoredEntry>
    fun markProcessed(entry: StoredEntry, error: String? = null)

}

data class StoredEntry(val id: Int, val created: Instant, val entry: Entry, val submitted: Instant?, val error: String?)
