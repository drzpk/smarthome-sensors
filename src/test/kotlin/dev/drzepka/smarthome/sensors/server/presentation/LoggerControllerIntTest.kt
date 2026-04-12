package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.CreateLoggerRequest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.LoggerResource
import dev.drzepka.smarthome.sensors.server.application.dto.logger.UpdateLoggerRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class LoggerControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `should return empty list when no loggers exist`() = testApp { client ->
        val response = client.get("/api/loggers")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        then(response.body<List<LoggerResource>>()).isEmpty()
    }

    @Test
    fun `should create logger and return generated password`() = testApp { client ->
        val response = client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody(CreateLoggerRequest().apply {
                name = "garage-sensor"
                description = "Garage temperature sensor"
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.Created)
        val body = response.body<LoggerResource>()
        then(body.id).isGreaterThan(0)
        then(body.name).isEqualTo("garage-sensor")
        then(body.password).isNotNull.isNotEmpty
    }

    @Test
    fun `should return 422 when logger name is missing`() = testApp { client ->
        val response = client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody(CreateLoggerRequest().apply { description = "desc" })
        }

        then(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should list loggers`() = testApp { client ->
        createLogger(client, "logger-one")
        createLogger(client, "logger-two")

        val response = client.get("/api/loggers")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<List<LoggerResource>>()
        then(body).hasSize(2)
        then(body.map { it.name }).containsExactlyInAnyOrder("logger-one", "logger-two")
    }

    @Test
    fun `should get logger by id`() = testApp { client ->
        val created = createLogger(client, "sensor-01")

        val response = client.get("/api/loggers/${created.id}")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<LoggerResource>()
        then(body.id).isEqualTo(created.id)
        then(body.name).isEqualTo("sensor-01")
        // Password is only returned on creation and reset
        then(body.password).isNull()
    }

    @Test
    fun `should return 404 for unknown logger`() = testApp { client ->
        val response = client.get("/api/loggers/9999")
        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should update logger`() = testApp { client ->
        val created = createLogger(client, "old-name")

        val response = client.patch("/api/loggers/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateLoggerRequest().apply { name = "new-name" })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        then(response.body<LoggerResource>().name).isEqualTo("new-name")
    }

    @Test
    fun `should delete logger`() = testApp { client ->
        val created = createLogger(client, "to-delete")

        val deleteResponse = client.delete("/api/loggers/${created.id}")
        then(deleteResponse.status).isEqualTo(HttpStatusCode.NoContent)

        val getResponse = client.get("/api/loggers/${created.id}")
        then(getResponse.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should reset logger password`() = testApp { client ->
        val created = createLogger(client, "reset-test")
        val originalPassword = created.password!!

        val response = client.delete("/api/loggers/${created.id}/password")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<LoggerResource>()
        then(body.password).isNotNull.isNotEqualTo(originalPassword)
    }

    private suspend fun createLogger(client: io.ktor.client.HttpClient, name: String): LoggerResource {
        return client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody(CreateLoggerRequest().apply {
                this.name = name
                description = "Integration test logger"
            })
        }.body()
    }
}
