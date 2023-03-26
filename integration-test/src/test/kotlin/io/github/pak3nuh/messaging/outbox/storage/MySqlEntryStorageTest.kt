package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.Entry
import io.github.pak3nuh.messaging.outbox.containers.createMySqlContainer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MySqlEntryStorageTest {

    @Test
    fun `should store entry`() {
        val host = container.host
        val port = container.firstMappedPort
        val entryStorage = SqlEntryStorage("jdbc:mysql://$host:$port/database", "mysql", "mysql")
        entryStorage.persist(
            Entry(
            byteArrayOf(1),
            byteArrayOf(2),
            "should-store-entry",
            mapOf(Pair("version","123"))
        )
        )

        val batch = entryStorage.getBatch()
        assertEquals(1, batch.count())
        val storedEntry = batch.first()
        assertEquals("should-store-entry", storedEntry.entry.id)
        assertArrayEquals(byteArrayOf(1), storedEntry.entry.key)
        assertArrayEquals(byteArrayOf(2), storedEntry.entry.value)
        assertEquals(mapOf(Pair("version","123")), storedEntry.entry.metadata)
    }

    companion object {
        @JvmStatic
        @Container
        val container = createMySqlContainer()
    }
}
