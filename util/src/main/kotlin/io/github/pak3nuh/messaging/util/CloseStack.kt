package io.github.pak3nuh.util

import io.github.pak3nuh.util.logging.KLoggerFactory

private val logger = KLoggerFactory.getLogger<CloseStack>()

class CloseStack: AutoCloseable {

    private var closeFun: () -> Unit = { }

    fun add(handle: AutoCloseable) {
        add(handle::close)
    }

    fun add(next: () -> Unit) {
        val previous = closeFun
        closeFun = {
            previous()
            try {
                next()
            } catch (e: Exception) {
                logger.error("Couldn't close $next", e)
            }
        }
    }

    override fun close() {
        closeFun()
    }
}