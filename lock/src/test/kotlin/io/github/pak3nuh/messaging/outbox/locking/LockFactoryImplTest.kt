package io.github.pak3nuh.messaging.outbox.locking

import io.github.pak3nuh.util.union.Either
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

class LockFactoryImplTest {
    private val provider = mockk<LockProvider>()
    private val sut = LockFactoryImpl(provider)

    @Test
    fun `should acquire lock without timeout`() {
        every { provider.obtainLock(any(), any()) } returns Either.First(mockk())

        val result = sut.tryLock("benfica", Duration.ZERO)

        Assertions.assertNotNull(result)
        verify { provider.obtainLock("benfica", null) }
    }

    @Test
    fun `should acquire lock with timeout`() {
        every { provider.obtainLock(any(), any()) } returns Either.First(mockk())

        val timeout = Duration.ofMinutes(1)
        val result = sut.tryLock("benfica", timeout)

        Assertions.assertNotNull(result)
        verify { provider.obtainLock("benfica", timeout) }
    }

    @Test
    fun `should return null on timeout`() {
        every { provider.obtainLock(any(), any()) } returns Either.Second(ProviderError(ErrorType.TIMEOUT))

        val timeout = Duration.ofMinutes(1)
        val result = sut.tryLock("benfica", timeout)

        Assertions.assertNull(result)
    }

    @Test
    fun `should return null on locked`() {
        every { provider.obtainLock(any(), any()) } returns Either.Second(ProviderError(ErrorType.LOCKED))

        val timeout = Duration.ofMinutes(1)
        val result = sut.tryLock("benfica", timeout)

        Assertions.assertNull(result)
    }

    @Test
    fun `should throw exception on unknown error`() {
        every { provider.obtainLock(any(), any()) } returns Either.Second(ProviderError(ErrorType.UNKNOWN))

        val timeout = Duration.ofMinutes(1)

        Assertions.assertThrowsExactly(LockException::class.java) {
            sut.tryLock("benfica", timeout)
        }
    }
}
