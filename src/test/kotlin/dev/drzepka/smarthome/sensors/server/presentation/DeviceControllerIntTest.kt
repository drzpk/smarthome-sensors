package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.device.CreateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.dto.device.DeviceResource
import dev.drzepka.smarthome.sensors.server.application.dto.device.UpdateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.dto.group.CreateGroupRequest
import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import dev.drzepka.smarthome.sensors.server.application.dto.logger.CreateLoggerRequest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.LoggerResource
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.PvMeasurement
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class DeviceControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `should create device`() = testApp { client ->
        val group = createGroup(client, "Test Group")

        val response = client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody(CreateDeviceRequest().apply {
                name = "thermometer-01"
                description = "Main thermometer"
                type = "temperature"
                mac = "AA:BB:CC:DD:EE:FF"
                groupId = group.id
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<DeviceResource>()
        then(body.id).isGreaterThan(0)
        then(body.name).isEqualTo("thermometer-01")
        then(body.type).isEqualTo("temperature")
        then(body.group!!.id).isEqualTo(group.id)
    }

    @Test
    fun `should return 422 when device name is missing`() = testApp { client ->
        val group = createGroup(client, "Test Group")

        val response = client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody(CreateDeviceRequest().apply {
                description = "desc"
                type = "temperature"
                mac = "AA:BB:CC:DD:EE:FF"
                groupId = group.id
            })
        }

        then(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should list devices`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        createDevice(client, "device-a", group.id)
        createDevice(client, "device-b", group.id)

        val response = client.get("/api/devices")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<List<DeviceResource>>()
        then(body).hasSize(2)
        then(body.map { it.name }).containsExactlyInAnyOrder("device-a", "device-b")
    }

    @Test
    fun `should get device by id`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        val created = createDevice(client, "sensor-01", group.id)

        val response = client.get("/api/devices/${created.id}")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<DeviceResource>()
        then(body.id).isEqualTo(created.id)
        then(body.name).isEqualTo("sensor-01")
    }

    @Test
    fun `should return 404 for unknown device`() = testApp { client ->
        val response = client.get("/api/devices/9999")
        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should update device`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        val created = createDevice(client, "old-name", group.id)

        val response = client.patch("/api/devices/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateDeviceRequest().apply { name = "new-name" })
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        then(response.body<DeviceResource>().name).isEqualTo("new-name")
    }

    @Test
    fun `should return 404 when updating unknown device`() = testApp { client ->
        val response = client.patch("/api/devices/9999") {
            contentType(ContentType.Application.Json)
            setBody(UpdateDeviceRequest().apply { name = "new-name" })
        }

        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should delete device`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        val created = createDevice(client, "to-delete", group.id)

        val deleteResponse = client.delete("/api/devices/${created.id}")
        then(deleteResponse.status).isEqualTo(HttpStatusCode.NoContent)

        val getResponse = client.get("/api/devices/${created.id}")
        then(getResponse.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should return 404 when deleting unknown device`() = testApp { client ->
        val response = client.delete("/api/devices/9999")
        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should return 404 for live data when no measurement has been submitted`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        val device = createDevice(client, "pv-sensor", group.id)

        val response = client.get("/api/devices/${device.id}/live")

        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should return live data after PV measurement is submitted`() = testApp { client ->
        val group = createGroup(client, "Test Group")
        val device = createDevice(client, "pv-sensor", group.id, type = "pv")
        val logger = createLogger(client)
        val phase = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f)

        client.post("/api/measurements") {
            basicAuth(logger.id.toString(), logger.password!!)
            contentType(ContentType.Application.Json)
            setBody(CreateMeasurementsRequest().apply {
                measurements.add(PvMeasurement(
                    deviceId = device.id,
                    time = null,
                    totalPower = 3000,
                    energyToday = BigDecimal("12.5"),
                    energyTotal = BigDecimal("1500.0"),
                    phaseA = phase,
                    phaseB = phase,
                    phaseC = phase,
                    pv1 = null,
                    pv2 = null
                ))
            })
        }

        val response = client.get("/api/devices/${device.id}/live")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<Map<String, Any>>()
        then(body["energy_today"] as Double).isEqualTo(12.5)
    }

    private suspend fun createGroup(client: HttpClient, name: String): GroupResource {
        return client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody(CreateGroupRequest().apply { this.name = name; description = "desc" })
        }.body()
    }

    private suspend fun createDevice(
        client: HttpClient,
        name: String,
        groupId: Int,
        type: String = "temperature"
    ): DeviceResource {
        return client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody(CreateDeviceRequest().apply {
                this.name = name
                description = "A sensor"
                this.type = type
                mac = "AA:BB:CC:DD:EE:FF"
                this.groupId = groupId
            })
        }.body()
    }

    private suspend fun createLogger(client: HttpClient): LoggerResource {
        return client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody(CreateLoggerRequest().apply { name = "test-logger"; description = "desc" })
        }.body()
    }

    private fun HttpRequestBuilder.basicAuth(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
