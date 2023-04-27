package io.github.pak3nuh.messaging.outbox

/**
 * The output sink for entries to be processed
 */
interface Sink: AutoCloseable {
    /**
     * Submits an entry to a sink interface.
     * If this process fails, it gets sent to a [ErrorHandler] to decide the [entry] outcome.
     * [entry] Entry to submit
     */
    fun submit(entry: Entry)
}