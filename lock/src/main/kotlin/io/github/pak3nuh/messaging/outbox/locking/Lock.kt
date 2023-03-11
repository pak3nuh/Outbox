package io.github.pak3nuh.messaging.outbox.locking

import java.time.Duration

interface Lock: AutoCloseable {
    val id: String
}

interface LockFactory {
    /**
     * Tries to obtain the lock.
     * @param id The lock ID.
     * @param timeout The timeout in seconds to acquire the lock.
     * @throws LockException If there is an unknown issue while acquiring the lock.
     * @return A [Lock] object if it is successful or null if the lock could not be
     *         acquired by being already locked or timed out
     */
    @Throws(LockException::class)
    fun tryLock(id: String, timeout: Duration): Lock?
}


class LockFactoryImpl(private val provider: LockServiceProvider): LockFactory {
    override fun tryLock(id: String, timeout: Duration): Lock? {
        return provider.obtainLock(id, timeout).fold(
            { LockImpl(id, it) },
            {
                when (it.type) {
                    ErrorType.TIMEOUT, ErrorType.LOCKED -> null
                    ErrorType.UNKNOWN -> throw LockException("Failed to acquire lock", it.cause)
                }
            }
        )
    }

}

private class LockImpl(override val id: String, private val providerLock: ProviderLock) : Lock {
    override fun close() {
        providerLock.close()
    }
}

class LockException(message: String? = null, cause: Throwable? = null): RuntimeException(message, cause)
