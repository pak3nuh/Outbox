package io.github.pak3nuh.messaging.outbox.containers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

/**
 * Create a postgres container.
 * See [createLiquibaseContainer] for schema initialization.
 */
fun createPgContainer(dbName: String, dbUser:String, dbPass: String): GenericContainer<*> {
    return GenericContainer("postgres:15")
        .withEnv(mapOf(
            "POSTGRES_PASSWORD" to dbUser,
            "POSTGRES_USER" to dbPass,
            "POSTGRES_DB" to dbName
        ))
        .withExposedPorts(5432)
}

/**
 * Create a mysql container initialized with the database schema with the settings:
 * Username: mysql
 * Password: mysql
 * Database: database
 */
fun createMySqlContainer(): GenericContainer<*> {
    return GenericContainer("mysql:8.0.32")
        .withEnv(mapOf(
            "MYSQL_ROOT_PASSWORD" to "mysql",
            "MYSQL_USER" to "mysql",
            "MYSQL_PASSWORD" to "mysql",
            "MYSQL_DATABASE" to "database"
        ))
        .withExposedPorts(3306)
        .withFileSystemBind("src/test/resources/init-mysql.sql", "/docker-entrypoint-initdb.d/init.sql")
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
fun createLiquibaseContainer(dbUrl: String, dbContainer: GenericContainer<*>, dbUser: String, dbPass: String, timeout: Long = 30): GenericContainer<*> {
    return GenericContainer("liquibase/liquibase:4.21")
        .withCommand(
            "--url=jdbc:$dbUrl",
            "--changeLogFile=changelog.xml",
            "--username=$dbUser",
            "--password=$dbPass",
            "--log-level=DEBUG",
            "update"
        )
        .dependsOn(dbContainer)
        .withFileSystemBind("../liquibase/database-schema.xml","/liquibase/changelog/changelog.xml")
        .waitingFor(Wait.forLogMessage(".*Command execution complete.*", 1)
            .withStartupTimeout(Duration.ofSeconds(timeout)))
}
