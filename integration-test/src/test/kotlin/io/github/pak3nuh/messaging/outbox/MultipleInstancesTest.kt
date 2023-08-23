package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.builder.OutboxBuilder
import io.github.pak3nuh.messaging.outbox.containers.createLiquibaseContainer
import io.github.pak3nuh.messaging.outbox.containers.createPgContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MultipleInstancesTest {
    @Test
    fun `should build multiple outbox instances`() {
        val host = dbContainer.host
        val port = dbContainer.firstMappedPort
        val sink1 = CollectorSink()
        val errorHandler = NoErrorsHandler()
        val outbox1 = OutboxBuilder.newBuilder()
            .withSink(sink1)
            .withSqlStorage("jdbc:postgresql://$host:$port/database", "postgres", "postgres", "outbox1")
            .withErrorHandler(errorHandler)
            .build()

        val sink2 = CollectorSink()
        val outbox2 = OutboxBuilder.newBuilder()
            .withSink(sink2)
            .withSqlStorage("jdbc:postgresql://$host:$port/database", "postgres", "postgres", "outbox2")
            .withErrorHandler(errorHandler)
            .build()

        outbox1.submit(Entry(byteArrayOf(), byteArrayOf()))
        outbox1.processEntries()
        Assertions.assertEquals(1, sink1.entries.size)
        Assertions.assertEquals(0, sink2.entries.size)

        outbox2.submit(Entry(byteArrayOf(), byteArrayOf()))
        outbox2.processEntries()
        Assertions.assertEquals(1, sink2.entries.size)

        val entry1 = sink1.entries.first()
        val entry2 = sink2.entries.first()
        Assertions.assertEquals(entry1, entry2)
        Assertions.assertTrue(entry1 !== entry2)
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
        val liquibase1 = createLiquibaseContainer("jdbc:postgresql://db:5432/database", dbContainer, "postgres", "postgres", outboxTableName = "outbox1")
            .withNetwork(network)

        @JvmStatic
        @Container
        val liquibase2 = createLiquibaseContainer("jdbc:postgresql://db:5432/database", dbContainer, "postgres", "postgres", outboxTableName = "outbox2", forceRun = true)
            .withNetwork(network)
    }
}

private class CollectorSink : Sink {

    private val _entries = mutableListOf<Entry>()
    val entries: List<Entry>
        get() = _entries

    override fun submit(entry: Entry) {
        _entries.add(entry)
    }

    override fun close() {
        //NoOp
    }

}

private class NoErrorsHandler : ErrorHandler {
    override fun onSubmitError(
        erroredSystem: ErrorHandler.ErroredSystem,
        entry: Entry,
        cause: Exception
    ): ErrorHandler.Recovery {
        throw UnsupportedOperationException()
    }

}
