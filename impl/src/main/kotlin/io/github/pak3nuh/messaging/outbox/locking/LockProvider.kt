package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import java.time.Duration

interface LockServiceProvider {
    val provider: String
    fun obtainLock(id: String, timeout: Duration = Duration.ZERO): Either<ProviderLock, ProviderError>
}

interface ProviderLock: AutoCloseable

data class ProviderError(val type: ErrorType, val cause: Throwable? = null)

enum class ErrorType {
    TIMEOUT,
    LOCKED,
    UNKNOWN
}