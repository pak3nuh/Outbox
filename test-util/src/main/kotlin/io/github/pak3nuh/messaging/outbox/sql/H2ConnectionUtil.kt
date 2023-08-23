package io.github.pak3nuh.messaging.outbox.sql

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

object H2ConnectionUtil {

    init {
        org.h2.Driver.load()
    }

    fun create(
        dbName: String = "test",
        tableName: String = "stored_entries",
        properties: Map<String, String> = emptyMap()
    ): Connection {
        val connectionString = getConnectionString(dbName, tableName)
        val conProps = Properties()
        conProps.putAll(properties)
        return DriverManager.getConnection(connectionString, conProps)
    }

    private fun createInitScriptFile(tableName: String): Path {
        val tempFilePath = Files.createTempFile("test-init.sql", "")
        Files.writeString(tempFilePath, initTemplate(tableName))
        return tempFilePath
    }

    private fun getConnectionString(dbName: String, tableName: String): String {
        val tempFile = createInitScriptFile(tableName)
        val escapedPath = tempFile.toFile().absolutePath.replace("\\", "\\\\")
        return "jdbc:h2:mem:$dbName;INIT=runscript from '$escapedPath'"
    }

    private fun initTemplate(tableName: String) : String {
        return """
            create table if not exists $tableName(
                id int NOT NULL primary key auto_increment,
                created timestamp not null,
                "key" binary(1024) not null,
                "value" binary(1024) not null ,
                user_id varchar(1000) not null,
                metadata varchar(8000) not null,
                submitted timestamp,
                error varchar(8000)
            );

            create table if not exists application_locks(
                lock_id varchar(1000) not null primary key,
                locked_at timestamp
            );

            create index if not exists id_${tableName}_created ON $tableName(created asc);
        """.trimIndent()
    }
}

