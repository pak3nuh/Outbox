package io.github.pak3nuh.messaging.outbox.storage

import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.sql.Connection
import java.sql.DriverManager

@EnabledIfSystemProperty(named = "database", matches = "postgres")
class PostgresSqlEntryStorageTest: SqlEntryStorageTest() {

    override val storage by lazy {
        SqlEntryStorage("jdbc:postgresql://172.25.0.1:5432/postgres", "postgres", "postgres")
    }

    override val connection: Connection = DriverManager.getConnection("jdbc:postgresql://172.25.0.1:5432/postgres", "postgres", "postgres")
}