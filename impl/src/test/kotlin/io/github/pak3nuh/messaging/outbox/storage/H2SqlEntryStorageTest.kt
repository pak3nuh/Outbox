package io.github.pak3nuh.messaging.outbox.storage

import org.h2.Driver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.io.FileOutputStream
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager

class H2SqlEntryStorageTest: SqlEntryStorageTest() {

    override val storage by lazy {
        SqlEntryStorage("jdbc:h2:mem:test", null, null)
    }

    override val connection: Connection
        get() = dbConnection ?: error("Uninitialized")

    companion object {
        lateinit var escapedPath: String
        // need to hold a connection to keep db alive for all tests
        var dbConnection: Connection? = null
        @BeforeAll
        @JvmStatic
        fun setupDb() {
            Driver.load()
            val stream = Companion::class.java.classLoader.getResourceAsStream("init.sql") ?: error("Required file")
            val file = Files.createTempFile("test-init.sql", "").toFile()
            stream.use {
                FileOutputStream(file).use {
                    stream.transferTo(it)
                }
            }

            escapedPath = file.absolutePath.replace("\\", "\\\\")
            dbConnection = DriverManager.getConnection("jdbc:h2:mem:test;INIT=runscript from '$escapedPath'")
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            dbConnection = null
        }
    }

}