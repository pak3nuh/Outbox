package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration

private val logger: Logger = LoggerFactory.getLogger(SharedFilesystemProvider::class.java)

class SharedFilesystemProvider(private val lockFolder: Path) : LockServiceProvider {
    override val provider: String = "filesystem"
    override fun obtainLock(id: String, timeout: Duration): Either<ProviderLock, ProviderError> {
        return try {
            val lockFile: Path = lockFolder.resolve("$id-$provider.lock")
            ensureFileExists(lockFile)
            acquireLock(lockFile)?.let { Either.First(it) } ?: Either.Second(ProviderError(ErrorType.LOCKED))
        } catch (unknown: Exception) {
            Either.Second(ProviderError(ErrorType.UNKNOWN, unknown))
        }
    }

    private fun acquireLock(lockFile: Path): ProviderLock? {
        // todo respect the timeout
        return try {
            val fileChannel = FileChannel.open(lockFile, setOf(StandardOpenOption.APPEND))
            fileChannel.tryLock()?.let { FileChannelLock(it) }
        } catch (_: OverlappingFileLockException) {
            null
        }
    }

    private fun ensureFileExists(lockFile: Path) {
        try {
            logger.debug("Creating lock file $lockFile")
            Files.createFile(lockFile)
        } catch (_: FileAlreadyExistsException) {
            logger.debug("File already exists, ignoring error")
        }
    }
}

class FileChannelLock(private val lock: FileLock): ProviderLock {
    override fun close() {
        lock.close()
        lock.channel().close()
    }
}