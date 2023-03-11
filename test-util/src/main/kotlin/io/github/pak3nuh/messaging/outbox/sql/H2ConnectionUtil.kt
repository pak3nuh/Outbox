package io.github.pak3nuh.messaging.outbox.sql

import org.h2.Driver
import java.io.FileOutputStream
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

object H2ConnectionUtil {

    init {
        Driver.load()
    }

    fun create(properties: Map<String, String> = emptyMap()): Connection {
        val stream = javaClass.classLoader.getResourceAsStream("init.sql") ?: error("Required file")
        val file = Files.createTempFile("test-init.sql", "").toFile()
        stream.use {
            FileOutputStream(file).use {
                stream.transferTo(it)
            }
        }

        val escapedPath = file.absolutePath.replace("\\", "\\\\")
        val conProps = Properties()
        conProps.putAll(properties)
        return DriverManager.getConnection("jdbc:h2:mem:test;INIT=runscript from '$escapedPath'", conProps)
    }

}

