package io.github.pak3nuh.messaging.outbox.builder

import io.github.pak3nuh.messaging.outbox.*
import io.github.pak3nuh.messaging.outbox.locking.*
import io.github.pak3nuh.messaging.outbox.storage.EntryStorage
import io.github.pak3nuh.messaging.outbox.storage.SqlEntryStorage
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Builds [Outbox] instances.
 */
class OutboxBuilder internal constructor() {

    private lateinit var sink: Sink
    private lateinit var errorHandler: ErrorHandler
    private lateinit var entryStorage: EntryStorage

    /**
     * Provides a [Sink] implementation.
     */
    fun withSink(sink: Sink) = apply {
        this.sink = sink
    }

    /**
     * Provides a [ErrorHandler] implementation.
     */
    fun withErrorHandler(errorHandler: ErrorHandler) = apply {
        this.errorHandler = errorHandler
    }

    /**
     * Provides a SQL based [EntryStorage].
     */
    fun withSqlStorage(conStr: String, user: String?, pass: String?) = apply {
        entryStorage = SqlEntryStorage(conStr, user, pass)
    }

    /**
     * Builds the outbox instance.
     * Requires calling [withSink], [withErrorHandler] and [withSqlStorage] before.
     */
    fun build(): Outbox {
        return OutboxImpl(entryStorage, errorHandler, sink)
    }

    /**
     * Builds the outbox and wraps it on a [ExclusiveLooper] instance.
     * @see [build]
     */
    fun buildLooper(): ExclusiveLooperBuilder {
        val outbox = build()
        return ExclusiveLooperBuilder(outbox)
    }

    companion object {
        /**
         * Creates a new builder.
         */
        fun newBuilder(): OutboxBuilder = OutboxBuilder()
    }

}

/**
 * Builds [Outbox] instances wraped in [ExclusiveLooper].
 */
class ExclusiveLooperBuilder internal constructor(private val outbox: Outbox) {

    private var lockTimeout: Duration = Duration.ofSeconds(1)
    private var loopSleepTime: Duration = Duration.ofSeconds(1)
    private var executorServiceGetter: () -> ScheduledExecutorService = { Executors.newSingleThreadScheduledExecutor() }
    private lateinit var lockFactory: LockFactory

    /**
     * Provides the lock timeout. The default is 1 second.
     * @see [ExclusiveLooper]
     */
    fun withLockTimeout(duration: Duration) = apply {
        this.lockTimeout = duration
    }

    /**
     * Provides the loop sleep time. The default is 1 second.
     * @see [ExclusiveLooper]
     */
    fun withLoopSleepTime(duration: Duration) = apply {
        this.loopSleepTime = duration
    }

    /**
     * Provides the executor service. The default is a single threaded executor service.
     * @see [ExclusiveLooper]
     */
    fun withExecutorService(executorService: ScheduledExecutorService) = apply {
        this.executorServiceGetter = { executorService }
    }

    /**
     * Provides a SQL based lock factory.
     * @see [SqlLockProvider]
     */
    fun withSqlLockFactory(conStr: String, user: String?, pass: String?, tableName: String = "application_locks") = apply {
        this.lockFactory = LockFactoryImpl(SqlLockProvider(tableName, DriverManagerProvider(conStr, user, pass)))
    }

    /**
     * Provides a shared folder lock factory.
     * @see [SharedFilesystemLockProvider]
     */
    fun withSharedFileLockFactory(lockFileFolder: Path) = apply {
        this.lockFactory = LockFactoryImpl(SharedFilesystemLockProvider(lockFileFolder))
    }

    /**
     * Builds the looper instance.
     * Requires calling [withSqlLockFactory] or [withSharedFileLockFactory].
     */
    fun build(): ExclusiveLooper {
        return ExclusiveLooper(lockFactory, outbox::processEntries, executorServiceGetter(), loopSleepTime, lockTimeout) {
            outbox.close()
        }
    }

}