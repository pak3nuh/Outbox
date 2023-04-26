package io.github.pak3nuh.messaging.outbox.locking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.concurrent.thread

class SqlLockProviderTest {
    private val sut = SqlLockProvider("application_locks", H2ConManager())

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

    private fun getLock(id: String): ProviderLock {
        return sut.obtainLock(id).fold(
            { it },
            { Assertions.fail(it.cause) }
        )
    }

}
