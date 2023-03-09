package io.github.pak3nuh.messaging.outbox.storage

import io.github.pak3nuh.messaging.outbox.sql.H2ConnectionUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.sql.Connection

class H2SqlEntryStorageTest: SqlEntryStorageTest() {

    override val storage by lazy {
        SqlEntryStorage("jdbc:h2:mem:test", null, null)
    }

    override val connection: Connection
        get() = dbConnection ?: error("Uninitialized")

    companion object {
        // need to hold a connection to keep db alive for all tests
        var dbConnection: Connection? = null
        @BeforeAll
        @JvmStatic
        fun setupDb() {
            dbConnection = H2ConnectionUtil.create()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            dbConnection = null
        }
    }

}