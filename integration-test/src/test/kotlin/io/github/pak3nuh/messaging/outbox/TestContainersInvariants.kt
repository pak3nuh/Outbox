package io.github.pak3nuh.messaging.outbox

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

@Testcontainers
class TestContainersInvariants {

    @Container
    private val sampleContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse("hashicorp/http-echo"))
        .withCommand("""-text=success""")
        .withExposedPorts(5678)

    @Test
    fun `should talk to the container`() {
        val host = sampleContainer.host
        val port = sampleContainer.firstMappedPort

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("http://$host:$port/"))
            .build()

        val response = client.send(request, BodyHandlers.ofString())

        Assertions.assertEquals(200, response.statusCode())
        Assertions.assertEquals("success", response.body().trim())
    }
}