package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.Entry
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.entity.update
import org.ktorm.schema.Table
import org.ktorm.schema.bytes
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import java.time.Instant

class SqlEntryStorage(conStr: String, user: String?, pass: String?): EntryStorage {

    private val database = Database.connect(conStr, user = user, password = pass)

    private val storedEntriesTable = database.sequenceOf(StoredEntries)

    override fun persist(entry: Entry) {
        storedEntriesTable.add(PersistedEntry {
            created = Instant.now()
            key = entry.key
            value = entry.value
            userId = entry.id
        })
    }

    override fun getBatch(): Sequence<StoredEntry> {
        return storedEntriesTable.filter { it.submitted.isNotNull()  }
            .sortedBy { it.created }
            .map { StoredEntry(it.id, it.created, Entry(it.key, it.value, it.userId), it.submitted, it.error) }
            .asSequence()
    }

    override fun markProcessed(entry: StoredEntry, error: String?) {
        val found = storedEntriesTable.find { it.id eq entry.id } ?: error("Can't find entry with id ${entry.id}")
        found.error = error
        found.submitted = Instant.now()
        storedEntriesTable.update(found)
    }

    override fun close() {
        // noOp
    }
}

private object StoredEntries: Table<PersistedEntry>("stored_entries") {
    val id = int("id").primaryKey().bindTo { it.id }
    val created = timestamp("created").bindTo { it.created }
    val key = bytes("key").bindTo { it.key }
    val value = bytes("value").bindTo { it.value }
    val userId = varchar("user_id").bindTo { it.userId }
    val submitted = timestamp("submitted").bindTo { it.submitted }
    val error = varchar("error").bindTo { it.error }
}

private interface PersistedEntry: Entity<PersistedEntry> {
    companion object : Entity.Factory<PersistedEntry>()
    val id: Int
    var created: Instant
    var key: ByteArray
    var value: ByteArray
    var userId: String?
    var submitted: Instant?
    var error: String?
}
