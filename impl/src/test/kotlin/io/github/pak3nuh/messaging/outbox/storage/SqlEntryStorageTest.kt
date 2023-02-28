package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.Entry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.*

abstract class SqlEntryStorageTest {

    abstract val storage: SqlEntryStorage
    abstract val connection: Connection
    private val helper by lazy { Helper(connection) }

    @BeforeEach
    fun clearTable() {
        helper.clearTable()
    }

    @Test
    fun `should connect and close`() {
        storage.close()
    }

    @Test
    fun `should persist entry`() {
        val entry = createEntry()
        storage.persist(entry)
        val count = helper.countById(entry.id)

        assertEquals(1, count)
    }

    @Test
    fun `should get batch`() {
        for (i in 1..10) {
            storage.persist(createEntry())
        }

        val batch = storage.getBatch()
        assertEquals(10, batch.count())
    }

    @Test
    fun `should mark submitted`() {
        val entry = createEntry()
        storage.persist(entry)

        val stored = storage.getBatch().first()
        storage.markProcessed(stored)
        val processed = helper.getEntry(stored.id)

        assertNull(stored.submitted)
        assertNotNull(processed.submitted)
    }

    @Test
    fun `should get unsubmited only`() {
        for(i in 1..10) {
            storage.persist(createEntry())
        }

        val batch = storage.getBatch().toList()
        assertEquals(10, batch.count())

        batch.asSequence().take(5)
            .forEach { storage.markProcessed(it) }

        assertEquals(5, storage.getBatch().count())
    }

    private fun createEntry(): Entry {
        val id = UUID.randomUUID().toString().substring(10)
        return Entry(byteArrayOf(), byteArrayOf(), id)
    }
}

private class Helper(val connection: Connection) {
    fun clearTable() {
        connection.prepareStatement("truncate table stored_entries").use { it.execute() }
    }

    fun countById(userId: String): Int {
        val statement = connection.prepareStatement("select count(*) from stored_entries where user_id=?")
        statement.setString(1, userId)
        statement.executeQuery().use { resultSet ->
            resultSet.next()
            return resultSet.getInt(1)
        }
    }

    fun getEntry(id: Int): StoredEntry {
        val statement = connection.prepareStatement("select * from stored_entries where id=?")
        statement.setInt(1, id)
        statement.executeQuery().use { resultSet ->
            resultSet.next()
            return StoredEntry(
                id,
                resultSet.getTimestamp("created").toInstant(),
                Entry(
                    resultSet.getBytes("key"),
                    resultSet.getBytes("value"),
                    resultSet.getString("user_id")
                ),
                resultSet.getTimestamp("submitted")?.toInstant(),
                resultSet.getString("error")
            )
        }
    }
}
