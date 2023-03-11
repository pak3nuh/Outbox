package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.messaging.outbox.sql.H2ConnectionUtil
import java.sql.Connection
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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