package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.locking.LockFactory
import io.github.pak3nuh.util.logging.KLoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loops over an [loopAction] with exclusivity rights.
 *
 * @impl The [ExclusiveLooper] is not subject to garbage collection while active because it has a thread running in
 * background that keeps it alive.
 *
 * @param lockFactory The exclusive lock acquisition mechanism.
 * @param loopSleepTime The time the looper waits before scheduling the next run. It doesn't count execution time.
 * @param lockTimeout The time waiting for the lock before times out.
 * @param loopAction The exclusive action to run.
 */
class ExclusiveLooper(
    private val lockFactory: LockFactory,
    private val loopAction: () -> Unit,
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
    private val loopSleepTime: Duration = Duration.ofSeconds(1),
    private val lockTimeout: Duration = Duration.ofSeconds(1),
) {

    private val running = AtomicBoolean(false)

    fun startLoop() {
        if (running.compareAndSet(false, true)) {
            executorService.submit { processLoop() }
        }
    }

    internal fun processLoop() {
        if (running.get()) {
            try {
                logger.debug("Acquiring lock")
                val lock = lockFactory.tryLock("processing-loop", lockTimeout)
                if (lock != null) {
                    lock.use {
                        loopAction()
                    }
                } else {
                    logger.debug("Lock not obtained, skipping")
                }
            } catch (_: InterruptedException) {
                logger.info("Interrupted. Will exit loop.")
                endLoop()
                return
            }
            catch (exception: Exception) {
                logger.error("Error running process loop", exception)
            }
            executorService.schedule({ processLoop() }, loopSleepTime.toMillis(), TimeUnit.MILLISECONDS)
        } else {
            logger.info("Exiting process loop")
        }
    }

    fun endLoop() {
        running.set(false)
        executorService.shutdown()
    }

    private companion object {
        val logger = KLoggerFactory.getLogger<ExclusiveLooper>()
    }

}