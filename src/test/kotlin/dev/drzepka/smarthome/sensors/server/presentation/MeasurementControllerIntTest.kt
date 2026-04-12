package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.device.CreateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.dto.device.DeviceResource
import dev.drzepka.smarthome.sensors.server.application.dto.group.CreateGroupRequest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.CreateLoggerRequest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.LoggerResource
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsResponse
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.CreateMeasurementsRequestV2
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureDataDTO
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class MeasurementControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `v1 - should accept measurements and return counts`() = testApp { client ->
        val (loggerId, password, deviceId) = setup(client)

        val response = client.post("/api/measurements") {
            basicAuth(loggerId, password)
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequest().apply {
                measurements.add(CreateMeasurementsRequest.Measurement().apply {
                    this.deviceId = deviceId
                    temperature = BigDecimal("22.5")
                    humidity = BigDecimal("60.0")
                })
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<CreateMeasurementsResponse>()
        then(body.created).isEqualTo(1)
        then(body.errors).isEqualTo(0)
    }

    @Test
    fun `v1 - should return 401 without credentials`() = testApp { client ->
        val response = client.post("/api/measurements") {
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequest())
        }

        then(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `v1 - should return 401 with invalid credentials`() = testApp { client ->
        val (loggerId, _, _) = setup(client)

        val response = client.post("/api/measurements") {
            basicAuth(loggerId, "wrong-password")
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequest())
        }

        then(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `v2 - should accept measurements and return counts`() = testApp { client ->
        val (loggerId, password, deviceId) = setup(client)

        val response = client.post("/api/v2/measurements") {
            basicAuth(loggerId, password)
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequestV2().apply {
                measurements.add(CreateMeasurementsRequestV2.Measurement().apply {
                    this.deviceId = deviceId
                    data = TemperatureDataDTO(
                        temperature = BigDecimal("21.0"),
                        humidity = BigDecimal("55.0"),
                        batteryVoltage = BigDecimal("3.7"),
                        batteryLevel = 90
                    )
                })
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<CreateMeasurementsResponse>()
        then(body.created).isEqualTo(1)
        then(body.errors).isEqualTo(0)
    }

    @Test
    fun `v2 - should count validation errors`() = testApp { client ->
        val (loggerId, password, deviceId) = setup(client)

        val response = client.post("/api/v2/measurements") {
            basicAuth(loggerId, password)
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequestV2().apply {
                measurements.add(CreateMeasurementsRequestV2.Measurement().apply {
                    this.deviceId = deviceId
                    data = TemperatureDataDTO(
                        temperature = BigDecimal("999.0"),  // out of range
                        humidity = BigDecimal("55.0")
                    )
                })
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<CreateMeasurementsResponse>()
        then(body.errors).isEqualTo(1)
        then(body.created).isEqualTo(0)
    }

    @Test
    fun `v2 - should return 401 without credentials`() = testApp { client ->
        val response = client.post("/api/v2/measurements") {
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequestV2())
        }

        then(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    private suspend fun setup(client: HttpClient): Triple<String, String, Int> {
        val groupId = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody(CreateGroupRequest().apply { name = "Test Group"; description = "desc" })
        }.body<dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource>().id

        val device = client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody(CreateDeviceRequest().apply {
                name = "test-device"
                description = "desc"
                type = "temperature"
                mac = "AA:BB:CC:DD:EE:FF"
                this.groupId = groupId
            })
        }.body<DeviceResource>()

        val logger = client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody(CreateLoggerRequest().apply { name = "test-logger"; description = "desc" })
        }.body<LoggerResource>()

        return Triple(logger.id.toString(), logger.password!!, device.id)
    }

    private fun HttpRequestBuilder.basicAuth(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
