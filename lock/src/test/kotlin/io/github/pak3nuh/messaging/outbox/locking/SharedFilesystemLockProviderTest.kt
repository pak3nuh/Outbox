package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.logging.KLoggerFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*

private val logger: Logger = KLoggerFactory.getLogger<SharedFilesystemLockProviderTest>()

class SharedFilesystemLockProviderTest {

    private val lockFolder = Paths.get(System.getProperty("java.io.tmpdir"))
    private val provider = SharedFilesystemLockProvider(lockFolder)

    @Test
    fun `should acquire and release lock`() {
        provider.obtainLock(UUID.randomUUID().toString(), Duration.ZERO).fold(
            { it.close() },
            { fail(it.cause) }
        )
    }

    @Test
    fun `should fail when lock is acquired`() {
        val lockId = "acquire-existing-lock"
        val lock1: ProviderLock = provider.obtainLock(lockId, Duration.ZERO).fold(
            { it },
            { fail(it.cause) }
        )

        provider.obtainLock(lockId, Duration.ZERO).fold(
            { fail("should be locked") },
            {
                it.cause?.printStackTrace()
                assertEquals(ErrorType.LOCKED, it.type)
            }
        )

        lock1.close()
    }

    @Test
    fun `should tolerate existing files`() {
        val tempDirectory = Files.createTempDirectory(lockFolder, null)
        val provider = SharedFilesystemLockProvider(tempDirectory)
        assertEquals(1, tempDirectory.toFile().walk().count())

        val lockId = UUID.randomUUID().toString()
        val file = Files.createFile(tempDirectory.resolve("$lockId-${provider.provider}.lock"))
        assertEquals(2, tempDirectory.toFile().walk().count())
        logger.debug("Created file $file")
        val result = provider.obtainLock(lockId)

        assertInstanceOf(result::class.java, result)
        assertEquals(2, tempDirectory.toFile().walk().count())
    }
}