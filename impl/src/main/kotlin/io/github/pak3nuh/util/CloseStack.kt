package io.github.pak3nuh.util

import io.github.pak3nuh.util.logging.KLoggerFactory

private val logger = KLoggerFactory.getLogger<CloseStack>()

class CloseStack: AutoCloseable {

    private var closeFun: () -> Unit = { }

    fun add(handle: AutoCloseable) {
        add(handle::close)
    }

    fun add(closer: () -> Unit) {
        closeFun = {
            closeFun()
            try {
                closer()
            } catch (e: Exception) {
                logger.error("Couldn't close $closer", e)
            }
        }
    }

    override fun close() {
        closeFun()
    }
}