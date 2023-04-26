package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.storage.EntryStorage
import io.github.pak3nuh.util.CloseStack
import io.github.pak3nuh.util.MetadataKeys
import io.github.pak3nuh.util.logging.KLoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

private const val tryMetadataKey = MetadataKeys.outbox + ".try"

class OutboxImpl(
    private val storage: EntryStorage,
    private val errorHandler: ErrorHandler,
    private val sink: Sink
    ): Outbox {

    private val closeStack = CloseStack()
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val running = AtomicBoolean(false)

    init {
        with(closeStack) {
            add(storage)
            add(sink)
            add {
                running.set(false)
                executorService.shutdown()
            }
        }
    }
    override fun submit(entry: Entry) {
        val copy = HashMap<String, String>(entry.metadata)
        copy.compute(tryMetadataKey) { _, curr -> "${(curr?.toInt() ?: 0) + 1}" }
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

    override fun close() {
        closeStack.close()
    }

    private companion object {
        val logger = KLoggerFactory.getLogger<OutboxImpl>()
    }
}
