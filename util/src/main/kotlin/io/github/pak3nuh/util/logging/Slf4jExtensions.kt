package io.github.pak3nuh.util.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// can't add extensions on java static methods :(
object KLoggerFactory {
    inline fun <reified T> getLogger(): Logger = LoggerFactory.getLogger(T::class.java)
}

