package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.sql.H2ConManager
import io.github.pak3nuh.util.union.Either
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.concurrent.thread

class SqlProviderTest {
    private val sut = SqlProvider("application_locks", H2ConManager())

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

}
