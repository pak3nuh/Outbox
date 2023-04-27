package io.github.pak3nuh.messaging.outbox

import io.github.pak3nuh.messaging.outbox.storage.EntryStorage
import io.github.pak3nuh.messaging.outbox.storage.StoredEntry
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.time.Instant

class OutboxImplTest {

    private val storageMock = mockk<EntryStorage>()
    private val errorHandlerMock = mockk<ErrorHandler>()
    private val sinkMock = mockk<Sink>()
    private val sut = OutboxImpl(storageMock, errorHandlerMock, sinkMock)

    @Test
    fun `should close resources`() {
        every { storageMock.close() } just Runs
        every { sinkMock.close() } just Runs
        sut.close()

        verifyAll {
            storageMock.close()
            sinkMock.close()
        }
    }

    @Test
    fun `should persist data for the first time`() {
        val entrySlot = slot<Entry>()
        every { storageMock.persist(capture(entrySlot)) } just Runs

        val key = byteArrayOf()
        val value = byteArrayOf()
        sut.submit(Entry(key, value, "some id"))

        verify { storageMock.persist(any()) }
        val captured = entrySlot.captured
        assertArrayEquals(key, captured.key)
        assertArrayEquals(value, captured.value)
        assertEquals("some id", captured.id)
        assertEquals(mapOf(Pair("io.github.pak3nuh.messaging.outbox.try", "1")), captured.metadata)
    }

    @Test
    fun `should increment tries`() {
        every { storageMock.persist(any()) } just Runs
        val entry = Entry(
            byteArrayOf(),
            byteArrayOf(),
            metadata = mapOf(Pair("io.github.pak3nuh.messaging.outbox.try", "5")),
        )

        sut.submit(entry)

        verify {
            val expected = entry.copy(metadata = mapOf(Pair("io.github.pak3nuh.messaging.outbox.try", "6")))
            storageMock.persist(expected)
        }
    }

    @Test
    fun `should process entry with success`() {
        val storedEntry = StoredEntry(1, Instant.now(), Entry(byteArrayOf(), byteArrayOf()), null, null)
        every { storageMock.getBatch() } returns sequenceOf(storedEntry)
        every { sinkMock.submit(any()) } just Runs
        every { storageMock.markProcessed(any()) } just Runs

        sut.processEntries()

        verifyAll {
            storageMock.getBatch()
            sinkMock.submit(storedEntry.entry)
            storageMock.markProcessed(storedEntry)
        }
    }

    @Test
    fun `should fail sending to sink with no recovery`() {
        val storedEntry = StoredEntry(1, Instant.now(), Entry(byteArrayOf(), byteArrayOf()), null, null)
        every { storageMock.getBatch() } returns sequenceOf(storedEntry)
        val thrownException = IllegalStateException("illegal")
        every { sinkMock.submit(any()) } throws thrownException
        every { storageMock.markProcessed(any(), any<String>()) } just Runs
        every { errorHandlerMock.onSubmitError(any(), any(), any()) } returns ErrorHandler.Recovery.NONE

        sut.processEntries()

        verifyAll {
            storageMock.getBatch()
            sinkMock.submit(storedEntry.entry)
            errorHandlerMock.onSubmitError(ErrorHandler.ErroredSystem.SINK, storedEntry.entry, thrownException)
            storageMock.markProcessed(storedEntry, "illegal")
        }
    }

    @Test
    fun `should fail marking as processed with no recovery`() {
        val storedEntry = StoredEntry(1, Instant.now(), Entry(byteArrayOf(), byteArrayOf()), null, null)
        every { storageMock.getBatch() } returns sequenceOf(storedEntry)
        val thrownException = IllegalStateException("illegal")
        every { sinkMock.submit(any()) } just runs
        every { storageMock.markProcessed(any(), any()) } throws thrownException andThenJust runs
        every { errorHandlerMock.onSubmitError(any(), any(), any()) } returns ErrorHandler.Recovery.NONE

        sut.processEntries()

        verifyAll {
            storageMock.getBatch()
            sinkMock.submit(storedEntry.entry)
            storageMock.markProcessed(storedEntry, null)
            errorHandlerMock.onSubmitError(ErrorHandler.ErroredSystem.MARK_PROCESSED, storedEntry.entry, thrownException)
            storageMock.markProcessed(storedEntry, "illegal")
        }
    }

    @Test
    fun `should fail sending to sink with retry`() {
        val storedEntry = StoredEntry(1, Instant.now(), Entry(byteArrayOf(), byteArrayOf()), null, null)
        every { storageMock.getBatch() } returns sequenceOf(storedEntry)
        val thrownException = IllegalStateException("illegal")
        every { sinkMock.submit(any()) } throws thrownException
        every { storageMock.markProcessed(any(), any<String>()) } just Runs
        every { errorHandlerMock.onSubmitError(any(), any(), any()) } returns ErrorHandler.Recovery.RETRY
        every { storageMock.persist(any()) } just runs

        sut.processEntries()

        verifyAll {
            storageMock.getBatch()
            sinkMock.submit(storedEntry.entry)
            errorHandlerMock.onSubmitError(ErrorHandler.ErroredSystem.SINK, storedEntry.entry, thrownException)
            storageMock.markProcessed(storedEntry, "illegal")
            val newEntry = storedEntry.entry.copy(metadata = mapOf(Pair("io.github.pak3nuh.messaging.outbox.try", "1")))
            storageMock.persist(newEntry)
        }
    }
}