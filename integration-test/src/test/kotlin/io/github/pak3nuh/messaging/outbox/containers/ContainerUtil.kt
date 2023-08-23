package io.github.pak3nuh.messaging.outbox.containers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.util.UUID

private val dbLogger: Logger = LoggerFactory.getLogger("DbLogger")
private val liquibaseLogger: Logger = LoggerFactory.getLogger("LiquibaseLogger")

/**
 * Create a postgres container.
 * See [createLiquibaseContainer] for schema initialization.
 */
fun createPgContainer(dbName: String, dbUser:String, dbPass: String, timeout: Long = 30): GenericContainer<*> {
    return GenericContainer("postgres:15")
        .withEnv(mapOf(
            "POSTGRES_PASSWORD" to dbUser,
            "POSTGRES_USER" to dbPass,
            "POSTGRES_DB" to dbName
        ))
        .withExposedPorts(5432)
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1).withStartupTimeout(Duration.ofSeconds(timeout)))
        .withLogConsumer(Slf4jLogConsumer(dbLogger, true))
}

/**
 * Create a mysql container initialized with the database schema with the settings:
 * Username: mysql
 * Password: mysql
 * Database: database
 */
fun createMySqlContainer(dbName: String, dbUser:String, dbPass: String, timeout: Long = 30): GenericContainer<*> {
    return GenericContainer("mysql:8.0.32")
        .withEnv(mapOf(
            "MYSQL_ROOT_PASSWORD" to dbPass,
            "MYSQL_USER" to dbUser,
            "MYSQL_PASSWORD" to dbPass,
            "MYSQL_DATABASE" to dbName
        ))
        .withExposedPorts(3306)
        .waitingFor(Wait.forLogMessage(".*ready for connections[.].*port: 3306.*", 1).withStartupTimeout(Duration.ofSeconds(timeout)))
        .withLogConsumer(Slf4jLogConsumer(dbLogger, true))
}

/**
 * Creates a container that runs the liquibase schema against another container.
 * Despite depending on [dbContainer], this is only for startup. If we try to access the container for host and port
 * info, we will get an error because the container hasn't started yet. This can be mitigated by setting a network
 * alias.
 *
 * @param dbUrl URL to connect to the database.
 * @param dbContainer The container definition to wait on.
 * @param dbUser The database username.
 * @param dbPass The database password.
 * @param timeout The timeout to wait for the liquibase command to run.
 */
fun createLiquibaseContainer(
    dbUrl: String,
    dbContainer: GenericContainer<*>,
    dbUser: String,
    dbPass: String,
    timeout: Long = 30,
    installMySql: Boolean = false,
    outboxTableName: String = "stored_entries",
    locksTableName: String = "application_locks",
    forceRun: Boolean = false
): GenericContainer<*> {
    val envMap = mutableMapOf<String, String>()
    if (installMySql) {
        envMap["INSTALL_MYSQL"] = "true"
    }
    val liquibaseTableName = if (forceRun) {
        "db-changelog-${UUID.randomUUID()}"
    } else {
        "DATABASECHANGELOG"
    }
    return GenericContainer("liquibase/liquibase:4.21")
        .withCommand(
            "--url=$dbUrl",
            "--changeLogFile=changelog.xml",
            "--databaseChangeLogTableName=$liquibaseTableName",
            "--username=$dbUser",
            "--password=$dbPass",
            "--log-level=INFO",
            "update",
            "-DOUTBOX_TABLE_NAME=$outboxTableName",
            "-DLOCKS_TABLE_NAME=$locksTableName"
        )
        .withEnv(envMap)
        .dependsOn(dbContainer)
        // container is fast and test-containers is dumb. force wait to get correct exit code via API
        .withStartupCheckStrategy(IsRunningStartupCheckStrategy())
        .waitingFor(Wait.forLogMessage(".*Command execution complete.*", 1).withStartupTimeout(Duration.ofSeconds(timeout)))
        .withFileSystemBind("../liquibase/database-schema.xml","/liquibase/changelog/changelog.xml")
        .withLogConsumer(Slf4jLogConsumer(liquibaseLogger, true))
}
