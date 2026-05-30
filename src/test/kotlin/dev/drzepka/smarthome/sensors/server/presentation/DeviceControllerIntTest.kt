package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.device.DeviceResource
import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import dev.drzepka.smarthome.sensors.server.application.dto.logger.LoggerResource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.util.*

class DeviceControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `should create device`() = testApp { client ->
        val group = createGroup(client, "Test Group")

        val response = client.post("/api/devices") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "thermometer-01",
                    "description": "Main thermometer",
                    "type": "temperature",
                    "mac": "AA:BB:CC:DD:EE:FF",
                    "groupId": ${group.id}
                }
            """.trimIndent())
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
            setBody("""
                {
                    "description": "desc",
                    "type": "temperature",
                    "mac": "AA:BB:CC:DD:EE:FF",
                    "groupId": ${group.id}
                }
            """.trimIndent())
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
            setBody("""
                {
                    "name": "new-name"
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        then(response.body<DeviceResource>().name).isEqualTo("new-name")
    }

    @Test
    fun `should return 404 when updating unknown device`() = testApp { client ->
        val response = client.patch("/api/devices/9999") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "new-name"
                }
            """.trimIndent())
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

        client.post("/api/measurements") {
            basicAuth(logger.id.toString(), logger.password!!)
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "measurements": [
                        {
                            "type": "PV",
                            "mac": "${device.mac}",
                            "time": null,
                            "totalPower": 3000,
                            "energyToday": 12.5,
                            "energyTotal": 1500.0,
                            "phaseA": {"voltage": 230.0, "current": 4.3, "power": 1000, "frequency": 50.0},
                            "phaseB": {"voltage": 230.0, "current": 4.3, "power": 1000, "frequency": 50.0},
                            "phaseC": {"voltage": 230.0, "current": 4.3, "power": 1000, "frequency": 50.0},
                            "pv1": null,
                            "pv2": null
                        }
                    ]
                }
            """.trimIndent())
        }

        val response = client.get("/api/devices/${device.id}/live")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<Map<String, Any>>()
        then(body["energy_today"] as Double).isEqualTo(12.5)
    }

    private suspend fun createGroup(client: HttpClient, name: String): GroupResource {
        return client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "$name",
                    "description": "desc"
                }
            """.trimIndent())
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
            setBody("""
                {
                    "name": "$name",
                    "description": "A sensor",
                    "type": "$type",
                    "mac": "AA:BB:CC:DD:EE:FF",
                    "groupId": $groupId
                }
            """.trimIndent())
        }.body()
    }

    private suspend fun createLogger(client: HttpClient): LoggerResource {
        return client.post("/api/loggers") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "test-logger",
                    "description": "desc"
                }
            """.trimIndent())
        }.body()
    }

    private fun HttpRequestBuilder.basicAuth(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
