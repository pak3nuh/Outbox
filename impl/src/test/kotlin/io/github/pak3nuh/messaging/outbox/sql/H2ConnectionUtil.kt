package io.github.pak3nuh.messaging.outbox.sql

import io.github.pak3nuh.messaging.outbox.locking.SqlConnectionProvider
import io.github.pak3nuh.messaging.outbox.storage.H2SqlEntryStorageTest
import org.h2.Driver
import java.io.FileOutputStream
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object H2ConnectionUtil {
    fun create(properties: Map<String, String> = emptyMap()): Connection {
        Driver.load()
        val stream = H2SqlEntryStorageTest.Companion::class.java.classLoader.getResourceAsStream("init.sql") ?: error("Required file")
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

/**
 * Creates connections to H2 in memory database.
 */
class H2ConManager: SqlConnectionProvider {

    override fun getConnection(timeout: Duration?): Connection {
        val con = H2ConnectionUtil.create()
        if (null != timeout) {
            con.setNetworkTimeout(executor, timeout.toMillis().toInt())
        }
        return con
    }

    override fun release(connection: Connection) {
        connection.close()
    }

    companion object {
        private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    }
}
