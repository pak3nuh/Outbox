package io.github.pak3nuh.messaging.outbox.locking

import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class PostgresLockTest: AbstractSqlLockTest() {

    override val container: GenericContainer<*>
        get() = pgContainer

    override val driverManagerProvider: DriverManagerProvider
        get() {
            val host = container.host
            val port = container.firstMappedPort
            return DriverManagerProvider("jdbc:postgresql://$host:$port/postgres", "postgres", "postgres")
        }

    companion object {
        @JvmStatic
        @Container
        private val pgContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse("postgres:15"))
            .withEnv(mapOf(
                "POSTGRES_PASSWORD" to "postgres",
                "POSTGRES_USER" to "postgres",
                "POSTGRES_DB" to "postgres"
            ))
            .withExposedPorts(5432)
            .withFileSystemBind("src/test/resources/init-pg.sql", "/docker-entrypoint-initdb.d/init.sql")
    }
}
