package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.containers.createLiquibaseContainer
import io.github.pak3nuh.messaging.outbox.containers.createMySqlContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MySqlLockTest: AbstractSqlLockTest() {

    override val container: GenericContainer<*>
        get() = dbContainer

    override val driverManagerProvider: DriverManagerProvider
        get() {
            val host = container.host
            val port = container.firstMappedPort
            return DriverManagerProvider("jdbc:mysql://$host:$port/database", "mysql", "mysql")
        }

    companion object {
        val network = Network.SHARED

        @JvmStatic
        @Container
        val dbContainer = createMySqlContainer("database", "mysql", "mysql")
            .withNetwork(network)
            .withNetworkAliases("db")

        @JvmStatic
        @Container
        val liquibase = createLiquibaseContainer("jdbc:mysql://db:3306/database", dbContainer, "mysql", "mysql", installMySql = true)
            .withNetwork(network)
    }
}
