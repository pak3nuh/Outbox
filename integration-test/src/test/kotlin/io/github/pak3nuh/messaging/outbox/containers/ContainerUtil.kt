package io.github.pak3nuh.messaging.outbox.containers

import org.testcontainers.containers.GenericContainer

/**
 * Create a postgres container initialized with the database schema with the settings:
 * Username: postgres
 * Password: postgres
 * Database: postgres
 */
fun createPgContainer(): GenericContainer<*> {
    return GenericContainer("postgres:15")
        .withEnv(mapOf(
            "POSTGRES_PASSWORD" to "postgres",
            "POSTGRES_USER" to "postgres",
            "POSTGRES_DB" to "postgres"
        ))
        .withExposedPorts(5432)
        .withFileSystemBind("src/test/resources/init-pg.sql", "/docker-entrypoint-initdb.d/init.sql")
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
