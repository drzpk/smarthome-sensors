package dev.drzepka.smarthome.sensors.server.presentation

import com.influxdb.query.FluxRecord
import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.device.DeviceResource
import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import dev.drzepka.smarthome.sensors.server.application.dto.logger.LoggerResource
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.channels.toList
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class MeasurementControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `should accept measurements and return counts`() = testApp { client ->
        val (loggerId, password, device) = setup(client)

        val response = client.post("/api/measurements") {
            basicAuth(loggerId, password)
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "measurements": [
                        {
                            "type": "TEMPERATURE",
                            "mac": "${device.mac}",
                            "time": "2026-01-15T10:30:00Z",
                            "temperature": 21.0,
                            "humidity": 55.0,
                            "batteryVoltage": 3.7,
                            "batteryLevel": 90
                        }
                    ]
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<CreateMeasurementsResponse>()
        then(body.created).isEqualTo(1)
        then(body.errors).isEqualTo(0)

        measurementService.storeMeasurements()

        val records = queryMeasurements("temperature", device.id, "2026-01-15T00:00:00Z", "2026-01-16T00:00:00Z")
        val fieldMap = records.associate { it.field!! to it.value }
        then(records.first().time).isEqualTo(Instant.parse("2026-01-15T10:30:00Z"))
        then(fieldMap["temperature"]).isEqualTo(21.0)
        then(fieldMap["humidity"]).isEqualTo(55.0)
        then(fieldMap["battery_voltage"]).isEqualTo(3.7)
        then(fieldMap["battery_level"]).isEqualTo(90L)
    }

    @Test
    fun `should count validation errors`() = testApp { client ->
        val (loggerId, password, device) = setup(client)

        val response = client.post("/api/measurements") {
            basicAuth(loggerId, password)
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "measurements": [
                        {
                            "type": "TEMPERATURE",
                            "mac": "${device.mac}",
                            "time": null,
                            "temperature": 999.0,
                            "humidity": 55.0
                        }
                    ]
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<CreateMeasurementsResponse>()
        then(body.errors).isEqualTo(1)
        then(body.created).isEqualTo(0)
    }

    @Test
    fun `should return 401 without credentials`() = testApp { client ->
        val response = client.post("/api/measurements") {
            contentType(ContentType.Application.Json)
            setBody("""{"measurements": []}""")
        }

        then(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    @Test
    fun `should return 401 with invalid credentials`() = testApp { client ->
        val (loggerId, _, _) = setup(client)

        val response = client.post("/api/measurements") {
            basicAuth(loggerId, "wrong-password")
            contentType(ContentType.Application.Json)
            setBody("""{"measurements": []}""")
        }

        then(response.status).isEqualTo(HttpStatusCode.Unauthorized)
    }

    private suspend fun setup(client: HttpClient): Triple<String, String, DeviceResource> {
        val groupId = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Test Group",
                    "description": "desc"
                }
            """.trimIndent())
        }.body<GroupResource>().id

        val device = client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "test-device",
                    "description": "desc",
                    "type": "temperature",
                    "mac": "AA:BB:CC:DD:EE:FF",
                    "groupId": $groupId
                }
            """.trimIndent())
        }.body<DeviceResource>()

        val logger = client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "test-logger",
                    "description": "desc"
                }
            """.trimIndent())
        }.body<LoggerResource>()

        return Triple(logger.id.toString(), logger.password!!, device)
    }

    private suspend fun queryMeasurements(
        measurement: String,
        deviceId: Int,
        rangeStart: String,
        rangeStop: String
    ): List<FluxRecord> {
        Thread.sleep(500)
        val flux = """
            from(bucket: "$INFLUX_BUCKET")
              |> range(start: $rangeStart, stop: $rangeStop)
              |> filter(fn: (r) => r._measurement == "$measurement")
              |> filter(fn: (r) => r["device"] == "$deviceId")
        """.trimIndent()
        val client = createInfluxClient()
        val records = client.getQueryKotlinApi().query(flux, INFLUX_ORG).toList()
        client.close()
        return records
    }

    private fun HttpRequestBuilder.basicAuth(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
