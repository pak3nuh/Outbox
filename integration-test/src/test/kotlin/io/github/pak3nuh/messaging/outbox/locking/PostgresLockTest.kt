package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.containers.createPgContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class PostgresLockTest: AbstractSqlLockTest() {

    override val container: GenericContainer<*>
        get() = pgContainer

    override val driverManagerProvider: DriverManagerProvider
        get() {
            val host = container.host
            val port = container.firstMappedPort
            return DriverManagerProvider("jdbc:postgresql://$host:$port/postgres", "postgres", "postgres")
        }

    companion object {
        @JvmStatic
        @Container
        private val pgContainer: GenericContainer<*> = createPgContainer()
    }
}
