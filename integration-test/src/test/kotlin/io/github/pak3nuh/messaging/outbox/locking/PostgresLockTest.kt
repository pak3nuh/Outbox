package io.github.pak3nuh.messaging.outbox.locking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.concurrent.thread

@Testcontainers
class PostgresLockTest {

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
        @JvmStatic
        @Container
        private val pgContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse("postgres:15"))
            .withEnv(mapOf(
                "POSTGRES_PASSWORD" to "postgres",
                "POSTGRES_USER" to "postgres",
                "POSTGRES_DB" to "postgres"
            ))
            .withExposedPorts(5432)
            .withFileSystemBind("src/test/resources/init-pg.sql", "/docker-entrypoint-initdb.d/init.sql")

        lateinit var sut: LockFactory

        @JvmStatic
        @BeforeAll
        fun initContainer() {
            val host = pgContainer.host
            val port = pgContainer.firstMappedPort
            sut = LockFactoryImpl(SqlProvider(
                "application_locks",
                DriverManagerProvider("jdbc:postgresql://$host:$port/postgres", "postgres", "postgres")
            ))
        }
    }
}
