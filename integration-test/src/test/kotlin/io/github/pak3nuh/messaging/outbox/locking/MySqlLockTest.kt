package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.logging.KLoggerFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import kotlin.concurrent.thread

@Testcontainers
class MySqlLockTest {

    @Test
    fun `should obtain lock`() {
        val lock = getLock("obtain-lock")
        lock.close()
    }

    @Test
    fun `should wait lock release`() {
        try {
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
                val id = "lock-release"
                val lock1 = getLock(id)
                val lock2 = getLock(id)
            }
        } catch (error: AssertionError) {
            val message = error.cause?.message
            println(message)
            Assertions.assertTrue(message?.contains("Execution timed out in thread") ?: error("no message"))
        }
    }

    @Test
    fun `should not block different ids`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val lock1 = getLock("lock_id_1")
            val lock2 = getLock("lock_id_2")
        }
    }

    @Test
    fun `should release lock on connection close`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val id = "release-on-connection-close"
            val lock1 = getLock(id)
            thread { lock1.close() }
            val lock2 = getLock(id)
        }
    }

    private fun getLock(id: String): Lock {
        return sut.tryLock(id, Duration.ZERO) ?: error("Lock not acquired")
    }

    companion object {

        private val logger = KLoggerFactory.getLogger<MySqlLockTest>()

        @JvmStatic
        @Container
        private val container: GenericContainer<*> = GenericContainer("mysql:8.0.32")
            .withEnv(mapOf(
                "MYSQL_ROOT_PASSWORD" to "mysql",
                "MYSQL_USER" to "mysql",
                "MYSQL_PASSWORD" to "mysql",
                "MYSQL_DATABASE" to "database"
            ))
            .withExposedPorts(3306)
            .withFileSystemBind("src/test/resources/init-mysql.sql", "/docker-entrypoint-initdb.d/init.sql")

        lateinit var sut: LockFactory

        @JvmStatic
        @BeforeAll
        fun initContainer() {
            container.followOutput(Slf4jLogConsumer(logger))

            val host = container.host
            val port = container.firstMappedPort
            sut = LockFactoryImpl(SqlProvider(
                "application_locks",
                DriverManagerProvider("jdbc:mysql://$host:$port/database", "mysql", "mysql")
            ))
        }
    }
}
