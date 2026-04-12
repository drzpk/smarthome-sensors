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

class InfluxDBMeasurementRepositoryIntTest : BaseIntegrationTest() {

    @Test
    fun `should write measurement and read it back`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())

        val measurementTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val measurement = Measurement(
            createdAt = measurementTime,
            deviceId = 1,
            loggerId = 2,
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
              |> filter(fn: (r) => r["device"] == "1" and r["logger"] == "2")
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
        then(sampleRecord.values["device"]).isEqualTo("1")
        then(sampleRecord.values["logger"]).isEqualTo("2")
    }

    @Test
    fun `should write multiple measurements`(): Unit = runBlocking {
        val repository = InfluxDBMeasurementRepository(createInfluxManager())

        val measurements = (1..3).map { i ->
            Measurement(
                createdAt = Instant.now().minusSeconds(i.toLong()).truncatedTo(ChronoUnit.SECONDS),
                deviceId = i,
                loggerId = 1,
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
              |> filter(fn: (r) => r["logger"] == "1")
        """.trimIndent()

        val client = createInfluxClient()
        val records = client.getQueryKotlinApi().query(flux, INFLUX_ORG).toList()
        client.close()

        then(records).hasSize(3)
    }
}
