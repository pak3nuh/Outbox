package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import java.time.Duration

interface LockServiceProvider {
    val provider: String

    /**
     * Acquire a lock with a given [id]. The lock is held until it is explicitly released.
     * @param id The lock id.
     * @param timeout The timeout to acquire the lock, not how long the lock is held.
     */
    fun obtainLock(id: String, timeout: Duration? = null): Either<ProviderLock, ProviderError>
}

interface ProviderLock: AutoCloseable

data class ProviderError(val type: ErrorType, val cause: Throwable? = null)

enum class ErrorType {
    TIMEOUT,
    LOCKED,
    UNKNOWN
}