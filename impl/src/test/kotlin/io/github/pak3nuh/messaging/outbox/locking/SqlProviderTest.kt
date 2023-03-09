package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import kotlin.concurrent.thread

class SqlProviderTest {
    private val sut = SqlProvider("application_locks", DriverManagerConnection("jdbc:postgresql://172.25.240.1:5432/postgres", "postgres", "postgres"))

    @Test
    fun `should obtain lock`() {
        val lock = sut.obtainLock("obtain-lock").fold(
            { it },
            { Assertions.fail(it.cause) }
        )
        lock.close()
    }

    @Test
    fun `should wait lock release`() {
        assertThrows<AssertionError> {
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
                val id = "lock-release"
                val lock1 = (sut.obtainLock(id) as Either.First).first
                val lock2 = (sut.obtainLock(id) as Either.First).first
            }
        }
    }

    @Test
    fun `should not block different ids`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val lock1 = (sut.obtainLock("lock_id_1") as Either.First).first
            val lock2 = (sut.obtainLock("lock_id_2") as Either.First).first
        }
    }

    @Test
    fun `should release lock on connection close`() {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            val id = "release-on-connection-close"
            val lock1 = (sut.obtainLock(id) as Either.First).first
            thread { lock1.close() }
            val lock2 = (sut.obtainLock(id) as Either.First).first
        }
    }

    private companion object {
        var dbConnection: Connection? = null
        var dbConnection2: Connection? = null
        @BeforeAll
        @JvmStatic
        fun setupDb() {
//            dbConnection = H2ConnectionUtil.create()
            dbConnection = DriverManager.getConnection("jdbc:postgresql://172.25.240.1:5432/postgres", "postgres", "postgres")
            dbConnection2 = DriverManager.getConnection("jdbc:postgresql://172.25.240.1:5432/postgres", "postgres", "postgres")
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            dbConnection = null
            dbConnection2 = null
        }
    }
}
