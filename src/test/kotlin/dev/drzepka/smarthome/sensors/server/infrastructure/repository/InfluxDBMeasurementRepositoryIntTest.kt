package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

class InfluxDBMeasurementRepositoryIntTest : BaseIntegrationTest() {

    @Test
    fun `should write measurement and read it back`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())
        val deviceId = nextDeviceId()
        val loggerId = nextDeviceId()

        val measurementTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val measurement = Measurement(
            createdAt = measurementTime,
            deviceId = deviceId,
            loggerId = loggerId,
            groupId = 0,
            type = "temperature",
            fields = mapOf(
                "temperature" to BigDecimal("22.50"),
                "humidity" to BigDecimal("65.00"),
                "battery_voltage" to BigDecimal("3.700"),
                "battery_level" to 85
            )
        )

        repository.save(0, listOf(measurement))

        // Allow InfluxDB write buffer to flush
        Thread.sleep(500)

        val flux = """
            from(bucket: "$INFLUX_BUCKET")
              |> range(start: -5m)
              |> filter(fn: (r) => r._measurement == "temperature")
              |> filter(fn: (r) => r["device"] == "$deviceId" and r["logger"] == "$loggerId")
        """.trimIndent()

        val client = createInfluxClient()
        val records = client.getQueryKotlinApi().query(flux, INFLUX_ORG).toList()
        client.close()

        // One record per field
        then(records).isNotEmpty
        val fieldMap = records.associate { it.field!! to it.value }
        then(fieldMap["temperature"]).isEqualTo(22.50)
        then(fieldMap["humidity"]).isEqualTo(65.00)
        then(fieldMap["battery_voltage"]).isEqualTo(3.700)
        then(fieldMap["battery_level"]).isEqualTo(85L)

        val sampleRecord = records.first()
        then(sampleRecord.measurement).isEqualTo("temperature")
        then(sampleRecord.values["device"]).isEqualTo("$deviceId")
        then(sampleRecord.values["logger"]).isEqualTo("$loggerId")
    }

    @Test
    fun `should write multiple measurements`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())
        val loggerId = nextDeviceId()

        val measurements = (1..3).map { i ->
            Measurement(
                createdAt = Instant.now().minusSeconds(i.toLong()).truncatedTo(ChronoUnit.SECONDS),
                deviceId = nextDeviceId(),
                loggerId = loggerId,
                groupId = 0,
                type = "temperature",
                fields = mapOf("temperature" to BigDecimal("${20 + i}.00"), "humidity" to BigDecimal("50.00"))
            )
        }

        repository.save(0, measurements)

        Thread.sleep(500)

        val flux = """
            from(bucket: "$INFLUX_BUCKET")
              |> range(start: -5m)
              |> filter(fn: (r) => r._measurement == "temperature")
              |> filter(fn: (r) => r._field == "temperature")
              |> filter(fn: (r) => r["logger"] == "$loggerId")
        """.trimIndent()

        val client = createInfluxClient()
        val records = client.getQueryKotlinApi().query(flux, INFLUX_ORG).toList()
        client.close()

        then(records).hasSize(3)
    }

    @Test
    fun `findLatestMeasurementTime should return latest time for device`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())
        val deviceId = nextDeviceId()
        val t1 = Instant.now().minusSeconds(10).truncatedTo(ChronoUnit.SECONDS)
        val t2 = Instant.now().minusSeconds(5).truncatedTo(ChronoUnit.SECONDS)

        repository.save(0, listOf(measurement(deviceId, t1), measurement(deviceId, t2)))
        Thread.sleep(500)

        val result = repository.findLatestMeasurementTime(0, deviceId, Instant.now().minusSeconds(60))

        then(result).isEqualTo(t2)
    }

    @Test
    fun `findLatestMeasurementTime should return null when no measurements exist for device`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())

        val result = repository.findLatestMeasurementTime(0, nextDeviceId(), Instant.now().minusSeconds(60))

        then(result).isNull()
    }

    @Test
    fun `findLatestMeasurementTime should return null when measurement is before since`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())
        val deviceId = nextDeviceId()
        val oldTime = Instant.now().minusSeconds(120).truncatedTo(ChronoUnit.SECONDS)

        repository.save(0, listOf(measurement(deviceId, oldTime)))
        Thread.sleep(500)

        val result = repository.findLatestMeasurementTime(0, deviceId, Instant.now().minusSeconds(60))

        then(result).isNull()
    }

    @Test
    fun `findLatestMeasurementTime should not return measurements for other devices`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())
        val deviceId = nextDeviceId()
        val otherDeviceId = nextDeviceId()

        repository.save(0, listOf(measurement(deviceId, Instant.now().minusSeconds(5).truncatedTo(ChronoUnit.SECONDS))))
        Thread.sleep(500)

        val result = repository.findLatestMeasurementTime(0, otherDeviceId, Instant.now().minusSeconds(60))

        then(result).isNull()
    }

    private fun measurement(deviceId: Int, time: Instant): Measurement = Measurement(
        createdAt = time,
        deviceId = deviceId,
        loggerId = 99,
        groupId = 0,
        type = "temperature",
        fields = mapOf("temperature" to BigDecimal("21.00"), "humidity" to BigDecimal("50.00"))
    )

    companion object {
        private val deviceIdCounter = AtomicInteger(100)
        private fun nextDeviceId() = deviceIdCounter.getAndIncrement()
    }
}
