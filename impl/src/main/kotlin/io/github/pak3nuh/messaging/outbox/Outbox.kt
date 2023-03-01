package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.storage.EntryStorage
import io.github.pak3nuh.util.CloseStack
import io.github.pak3nuh.util.MetadataKey
import io.github.pak3nuh.util.logging.KLoggerFactory
import java.util.concurrent.Executors

interface Outbox : AutoCloseable {
    fun submit(entry: Entry)
    fun start()
}

private val tryMetadataKey = MetadataKey.outbox + "try"

class OutboxImpl(private val storage: EntryStorage, private val errorHandler: ErrorHandler, private val sink: Sink): Outbox {

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

    internal fun processEntries() {
        storage.getBatch().forEach {
                try {
                    sink.submit(it.entry)
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
                processEntries()
            } catch (_: InterruptedException) {
                logger.info("Interrupted")
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

interface ErrorHandler {
    fun onSubmitError(entry: Entry, cause: Exception): Recovery

    enum class Recovery {
        NONE,
        RETRY
    }
}

interface Sink: AutoCloseable {
    fun submit(entry: Entry)
}

data class Entry(val key: ByteArray, val value: ByteArray, val id: String = "", val metadata: Map<String, String> = emptyMap()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Entry

        if (!key.contentEquals(other.key)) return false
        if (!value.contentEquals(other.value)) return false
        if (id != other.id) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

}
