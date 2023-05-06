package io.github.pak3nuh.messaging.outbox.builder

import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.nio.file.Files

class OutboxBuilderTest{

    @Test
    fun `should build an outbox`() {
        OutboxBuilder.newBuilder()
            .withSink(mockk())
            .withErrorHandler(mockk())
            .withSqlStorage("jdbc:h2:mem:test", null, null)
            .build()
    }

    @Test
    fun `should build a looper`() {
        OutboxBuilder.newBuilder()
            .withSink(mockk())
            .withErrorHandler(mockk())
            .withSqlStorage("jdbc:h2:mem:test", null, null)
            .buildLooper()
            .withSharedFileLockFactory(Files.createTempDirectory(""))
            .build()
    }
}