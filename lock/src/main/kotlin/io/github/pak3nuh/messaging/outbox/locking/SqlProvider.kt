package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.CloseStack
import io.github.pak3nuh.util.logging.KLoggerFactory
import io.github.pak3nuh.util.union.Either
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.SQLTimeoutException
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Gets and releases connections.
 */
interface SqlConnectionProvider {
    /**
     * Get a connection and sets the [timeout] for operations.
     * @param timeout The timeout for executing statements.
     */
    fun getConnection(timeout: Duration?): Connection

    /**
     * Releases a connection.
     */
    fun release(connection: Connection)
}

/**
 * Obtains a lock on a specific row in an SQL table. This lock is held as long as the connection is active or
 * the lock is release.
 *
 * Table rows are not deleted after use. This class is designed to have known ids through the application.
 * This is meant to simplify implementation because locking single rows on update statements is far more
 * deterministic than locking tables on row insertion.
 */
class SqlProvider(private val tableName: String, private val connectionProvider: SqlConnectionProvider) : LockServiceProvider {
    override val provider: String = "sql"

    override fun obtainLock(id: String, timeout: Duration?): Either<ProviderLock, ProviderError> {
        return try {
            val connection = connectionProvider.getConnection(timeout)
            connection.autoCommit = false
            ensureId(id, connection)
            val sqlLock = SqlLock(connection, tableName, id, connectionProvider::release)
            sqlLock.lock()
            return Either.First(sqlLock)
        } catch (timeout: SQLTimeoutException) {
            Either.Second(ProviderError(ErrorType.TIMEOUT, timeout))
        } catch (unknown: Exception) {
            Either.Second(ProviderError(ErrorType.UNKNOWN, unknown))
        }
    }

    private fun ensureId(id: String, connection: Connection) {
        val insertId = "insert into $tableName(lock_id) values(?)"
        connection.prepareStatement(insertId).use {
            it.setString(1, id)
            try {
                logger.debug("Inserting row with id {}", id)
                it.executeUpdate()
                connection.commit()
            } catch (e: SQLException) {
                connection.rollback()
                logger.debug("Error executing, most likely ID exists. Increase log severity to see error.")
                logger.trace("Error message", e)
                // id duplicated, do nothing
            }
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SqlProvider::class.java)
    }

}

private class SqlLock(
    private val connection: Connection,
    private val tableName: String,
    private val id: String,
    private val onRelease: (Connection) -> Unit
): ProviderLock {

    private val closeStack = CloseStack()

    fun lock() {
        logger.debug("Locking with id {}", id)
        val selectStatement = getSelectStatement()
        val updateStatement = getUpdateStatement()
        val resultSet = selectStatement.executeQuery()
        closeStack.add(resultSet)
        closeStack.add(selectStatement)
        updateStatement.executeUpdate()
        closeStack.add(updateStatement)
        closeStack.add { connection.commit() }
    }

    private fun getSelectStatement(): PreparedStatement {
        val lockQuery = "select * from $tableName where lock_id=? for update"
        val lockStatement = connection.prepareStatement(lockQuery)
        lockStatement.setString(1, id)
        return lockStatement
    }

    private fun getUpdateStatement(): PreparedStatement {
        val lockQuery = "update $tableName set locked_at=? where lock_id=?"
        val lockStatement = connection.prepareStatement(lockQuery)
        lockStatement.setTimestamp(1, Timestamp.from(Instant.now()))
        lockStatement.setString(2, id)
        return lockStatement
    }

    override fun close() {
        try {
            logger.debug("Releasing Lock with id {}", tableName)
            closeStack.close()
        } finally {
            onRelease(connection)
        }
    }

    private companion object {
        val logger = KLoggerFactory.getLogger<SqlLock>()
    }
}

class DriverManagerProvider(private val conStr: String, private val username: String?, private val password: String?): SqlConnectionProvider {

    private val executor = Executors.newSingleThreadExecutor()

    override fun getConnection(timeout: Duration?): Connection {
        val connection = if (username != null && password != null) {
            DriverManager.getConnection(conStr, username, password)
        } else {
            DriverManager.getConnection(conStr)
        }
        if (timeout != null) {
            connection.setNetworkTimeout(executor, timeout.toMillis().toInt())
        }
        return connection
    }

    override fun release(connection: Connection) {
        logger.debug("Releasing connection")
        connection.close()
    }

    private companion object {
        val logger = KLoggerFactory.getLogger<DriverManagerProvider>()
    }
}
