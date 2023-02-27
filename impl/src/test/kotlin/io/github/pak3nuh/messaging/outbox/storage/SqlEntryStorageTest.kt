package io.github.pak3nuh.messaging.outbox.storage

import org.h2.Driver
import org.junit.jupiter.api.Test
import java.io.FileOutputStream
import java.nio.file.Files

class SqlEntryStorageTest {

    private val driver = Driver.load()
    private val storage by lazy {
        SqlEntryStorage("jdbc:h2:mem:sql-entry-storage-test;INIT=runscript from '$escapedPath'", null, null)
    }

    @Test
    fun `should connect and close`() {
        storage.close()
    }

    companion object {
        val escapedPath: String
        init {
            val stream = Companion::class.java.classLoader.getResourceAsStream("init.sql") ?: error("Required file")
            val file = Files.createTempFile("test-init.sql", "").toFile()
            stream.use {
                FileOutputStream(file).use {
                    stream.transferTo(it)
                }
            }

            escapedPath = file.absolutePath.replace("\\", "\\\\")
        }
    }
}