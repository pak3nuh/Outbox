package io.github.pak3nuh.messaging.outbox.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.pak3nuh.messaging.outbox.Entry
import io.github.pak3nuh.util.logging.KLoggerFactory
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNull
import org.ktorm.entity.Entity
import org.ktorm.entity.EntitySequence
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

class SqlEntryStorage(conStr: String, user: String?, pass: String?) : EntryStorage {

    private val database = Database.connect(conStr, user = user, password = pass)

    private val storedEntriesTable = database.sequenceOf(EntryTable)

    private val codec = Gson()

    override fun persist(entry: Entry) {
        val entity = EntryEntity {
            created = Instant.now()
            key = entry.key
            value = entry.value
            userId = entry.id
            metadata = codec.toJson(entry.metadata)
        }
        storedEntriesTable.add(entity)
        logger.debug("Inserted entry with ID {}", entity.id)
    }

    override fun getBatch(): Sequence<StoredEntry> {
        return storedEntriesTable.filter { it.submitted.isNull() }
            .sortedBy { it.created }
            .mapToStored()
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

    private fun EntitySequence<EntryEntity, EntryTable>.mapToStored() =
        this.map {
            StoredEntry(
                it.id,
                it.created,
                Entry(
                    it.key,
                    it.value,
                    it.userId,
                    codec.fromJson(it.metadata, MapToken)
                ),
                it.submitted,
                it.error
            )
        }.asSequence()

    private companion object {
        val logger = KLoggerFactory.getLogger<SqlEntryStorage>()
    }

    private object MapToken: TypeToken<Map<String, String>>()
}

private object EntryTable : Table<EntryEntity>("stored_entries") {
    val id = int("id").primaryKey().bindTo { it.id }
    val created = timestamp("created").bindTo { it.created }
    val key = bytes("key").bindTo { it.key }
    val value = bytes("value").bindTo { it.value }
    val userId = varchar("user_id").bindTo { it.userId }
    val metadata = varchar("metadata").bindTo { it.metadata }
    val submitted = timestamp("submitted").bindTo { it.submitted }
    val error = varchar("error").bindTo { it.error }
}

private interface EntryEntity : Entity<EntryEntity> {
    companion object : Entity.Factory<EntryEntity>()

    val id: Int
    var created: Instant
    var key: ByteArray
    var value: ByteArray
    var userId: String
    var metadata: String
    var submitted: Instant?
    var error: String?
}
