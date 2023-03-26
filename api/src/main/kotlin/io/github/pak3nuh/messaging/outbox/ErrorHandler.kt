package io.github.pak3nuh.messaging.outbox

/**
 * Error handler invoked whenever an error occurs when processing an entity.
 */
interface ErrorHandler {
    /**
     * Invoked when [Sink.submit] returns an exception.
     */
    fun onSubmitError(entry: Entry, cause: Exception): Recovery

    /**
     * The recovery after an error is encountered.
     */
    enum class Recovery {
        /**
         * The message is dropped.
         */
        NONE,

        /**
         * The message is put on the end of the batch and processed again.
         */
        RETRY
    }
}