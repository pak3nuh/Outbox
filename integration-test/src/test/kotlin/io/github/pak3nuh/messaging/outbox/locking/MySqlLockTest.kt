package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.containers.createMySqlContainer
import io.github.pak3nuh.util.logging.KLoggerFactory
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MySqlLockTest: AbstractSqlLockTest() {

    override val container: GenericContainer<*>
        get() = Companion.container

    override val driverManagerProvider: DriverManagerProvider
        get() {
            val host = container.host
            val port = container.firstMappedPort
            return DriverManagerProvider("jdbc:mysql://$host:$port/database", "mysql", "mysql")
        }

    companion object {

        @JvmStatic
        @Container
        private val container: GenericContainer<*> = createMySqlContainer()

        @JvmStatic
        @BeforeAll
        fun initContainer() {
            container.followOutput(Slf4jLogConsumer(KLoggerFactory.getLogger<MySqlLockTest>()))
        }
    }
}
