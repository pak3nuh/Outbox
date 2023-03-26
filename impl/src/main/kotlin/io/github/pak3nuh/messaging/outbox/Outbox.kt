package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.locking.LockFactory
import io.github.pak3nuh.messaging.outbox.storage.EntryStorage
import io.github.pak3nuh.util.CloseStack
import io.github.pak3nuh.util.MetadataKey
import io.github.pak3nuh.util.logging.KLoggerFactory
import java.time.Duration
import java.util.concurrent.Executors

private val tryMetadataKey = MetadataKey.outbox + "try"

// todo export timeouts and sleeps to parameters
// todo export loop mechanic to interface
class OutboxImpl(
    private val storage: EntryStorage,
    private val errorHandler: ErrorHandler,
    private val sink: Sink,
    private val lockFactory: LockFactory): Outbox {

    private val closeStack = CloseStack()
    private val executorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var running = false

    init {
        executorService.submit { processLoop() }
        with(closeStack) {
            add(storage)
            add(sink)
            add {
                running = false
                executorService.shutdown()
            }
        }
    }
    override fun submit(entry: Entry) {
        val copy = HashMap<String, String>(entry.metadata)
        copy.compute(tryMetadataKey.toString()) { _, curr -> "${(curr?.toInt() ?: 0) + 1}" }
        storage.persist(entry.copy(metadata = copy))
    }

    private fun processEntries() {
        val entryList = storage.getBatch().toList()
        logger.debug("Processing entry batch with {} records.", entryList.size)
        entryList.forEach {
                try {
                    logger.trace("Submitting user entry with id {}.", it.entry.id)
                    sink.submit(it.entry)
                    logger.trace("Marking entry {} as processed.", it.entry.id)
                    storage.markProcessed(it)
                } catch (exception: Exception) {
                    val recovery = errorHandler.onSubmitError(it.entry, exception)
                    logger.error("Couldn't process entry {}. Recovery with {}", it, recovery)
                    storage.markProcessed(it, exception.message)
                    when(recovery) {
                        ErrorHandler.Recovery.RETRY -> this.submit(it.entry)
                        ErrorHandler.Recovery.NONE -> { }
                    }
                }
            }
    }

    override fun start() {
        running = true
    }

    private fun processLoop() {
        if (running) {
            try {
                logger.debug("Acquiring lock")
                val lock = lockFactory.tryLock("processing-loop", Duration.ofSeconds(1))
                if (lock != null) {
                    processEntries()
                } else {
                    logger.debug("Lock not obtained, skipping")
                }
            } catch (_: InterruptedException) {
                logger.info("Interrupted")
                running = false
                return
            }
            catch (exception: Exception) {
                logger.error("Error running process loop", exception)
            }
        }
        Thread.sleep(1_000)
        executorService.submit { processLoop() }
    }

    override fun close() {
        closeStack.close()
    }

    private companion object {
        val logger = KLoggerFactory.getLogger<OutboxImpl>()
    }
}
