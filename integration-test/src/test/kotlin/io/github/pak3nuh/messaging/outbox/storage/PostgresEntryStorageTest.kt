package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.Entry
import io.github.pak3nuh.messaging.outbox.containers.createLiquibaseContainer
import io.github.pak3nuh.messaging.outbox.containers.createPgContainer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class PostgresEntryStorageTest {

    @Test
    fun `should store entry`() {
        val host = dbContainer.host
        val port = dbContainer.firstMappedPort
        val entryStorage = SqlEntryStorage("jdbc:postgresql://$host:$port/database", "postgres", "postgres")
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
        val network = Network.SHARED

        @JvmStatic
        @Container
        val dbContainer = createPgContainer("database", "postgres", "postgres")
            .withNetwork(network)
            .withNetworkAliases("db")

        @JvmStatic
        @Container
        val liquibase = createLiquibaseContainer("jdbc:postgresql://db:5432/database", dbContainer, "postgres", "postgres")
            .withNetwork(network)
    }
}
