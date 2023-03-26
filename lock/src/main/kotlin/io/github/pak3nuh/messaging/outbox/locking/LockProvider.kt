package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import java.time.Duration

/**
 * An abstraction over the actual locking mechanism chosen.
 * Semantics may vary slightly between providers but the general contract still holds.
 */
interface LockServiceProvider {
    /**
     * A key to identify the provider. Should be as unique as possible.
     */
    val provider: String

    /**
     * Acquire a lock with a given [id]. The lock is held until it is explicitly released.
     * @param id The lock id.
     * @param timeout The timeout to acquire the lock, not how long the lock is held.
     * @return Returns [Either] a [ProviderLock] instance or a [ProviderError] if
     *          the lock couldn't be acquired. See [ErrorType] for more info.
     */
    fun obtainLock(id: String, timeout: Duration? = null): Either<ProviderLock, ProviderError>
}

/**
 * The actual specific lock instance. While this object is active, the lock is held, only
 * being release by calling [close] method.
 */
interface ProviderLock: AutoCloseable

/**
 * Holder object that represents a failed lock acquisition.
 */
data class ProviderError(val type: ErrorType, val cause: Throwable? = null)

enum class ErrorType {
    /**
     * Timeout while acquiring the lock. Some providers do not error out when the locks
     * exist, but wait for their turn to acquire it.
     */
    TIMEOUT,

    /**
     * When the lock is already acquired and is not possible to wait a timeout.
     */
    LOCKED,

    /**
     * The lock acquisition failed for an unknown reason.
     */
    UNKNOWN
}