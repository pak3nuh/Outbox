package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.containers.createLiquibaseContainer
import io.github.pak3nuh.messaging.outbox.containers.createPgContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class PostgresLockTest: AbstractSqlLockTest() {

    override val container: GenericContainer<*>
        get() = dbContainer

    override val driverManagerProvider: DriverManagerProvider
        get() {
            val host = container.host
            val port = container.firstMappedPort
            return DriverManagerProvider("jdbc:postgresql://$host:$port/postgres", "postgres", "postgres")
        }

    companion object {
        val network = Network.SHARED

        @JvmStatic
        @Container
        val dbContainer = createPgContainer("postgres", "postgres", "postgres")
            .withNetwork(network)
            .withNetworkAliases("db")

        @JvmStatic
        @Container
        val liquibase = createLiquibaseContainer("postgresql://db:5432/postgres", dbContainer, "postgres", "postgres")
            .withNetwork(network)
    }
}
