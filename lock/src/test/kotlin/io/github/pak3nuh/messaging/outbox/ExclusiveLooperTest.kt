package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.locking.Lock
import io.github.pak3nuh.messaging.outbox.locking.LockFactory
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val s = "processing-loop"

class ExclusiveLooperTest {
    private val lockFactoryMock = mockk<LockFactory>()
    private val loopActionMock = mockk<() -> Unit>()
    private val executorServiceMock = mockk<ScheduledExecutorService>()
    private val loopSleepTime = Duration.ofSeconds(2)
    private val lockTimeout = Duration.ofSeconds(3)
    private val lockId = "processing-loop"

    private val sut = ExclusiveLooper(lockFactoryMock, loopActionMock, executorServiceMock, loopSleepTime, lockTimeout)

    @Test
    fun `should not run unless started`() {
        sut.processLoop()

        verify(exactly = 0) { loopActionMock.invoke() }
    }

    @Test
    fun `should submit first action on start`() {
        every { executorServiceMock.submit(any()) } returns null
        sut.startLoop()

        verify { executorServiceMock.submit(any<Runnable>()) }
    }

    @Test
    fun `should close resources`() {
        every { executorServiceMock.shutdown() } just Runs
        sut.endLoop()

        verify { executorServiceMock.shutdown() }
    }

    @Test
    fun `should not run action if lock not acquired`() {
        every { executorServiceMock.submit(any()) } returns null
        every { executorServiceMock.schedule(any(), any(), any()) } returns null
        sut.startLoop()
        every { lockFactoryMock.tryLock(lockId, lockTimeout) } returns null

        sut.processLoop()

        verify(exactly = 0) { loopActionMock.invoke() }
        verify { executorServiceMock.schedule(any(), loopSleepTime.toMillis(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `should invoke action on lock acquired`() {
        every { executorServiceMock.submit(any()) } returns null
        every { executorServiceMock.schedule(any(), any(), any()) } returns null
        every { loopActionMock.invoke() } just Runs
        sut.startLoop()
        every { lockFactoryMock.tryLock(lockId, lockTimeout) } returns LockStub()

        sut.processLoop()

        verify { loopActionMock.invoke() }
        verify { executorServiceMock.schedule(any(), loopSleepTime.toMillis(), TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `should end processing on interrupted thread`() {
        every { executorServiceMock.submit(any()) } returns null
        every { executorServiceMock.shutdown() } just Runs
        sut.startLoop()
        every { lockFactoryMock.tryLock(lockId, lockTimeout) } returns LockStub()
        every { loopActionMock.invoke() } answers {
            Thread.currentThread().interrupt()
            throw InterruptedException()
        }

        sut.processLoop()

        verify { loopActionMock.invoke() }
        verify { executorServiceMock.shutdown() }
        verify(exactly = 0) { executorServiceMock.schedule(any(), loopSleepTime.toMillis(), TimeUnit.MILLISECONDS) }
    }
}

private class LockStub: Lock {
    override val id: String
        get() = "lock-stub"

    override fun close() {
        //noOp
    }
}