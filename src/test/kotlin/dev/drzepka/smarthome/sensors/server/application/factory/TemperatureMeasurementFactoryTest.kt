package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureMeasurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TemperatureMeasurementFactoryTest {

    private val factory = TemperatureMeasurementFactory()

    @Test
    fun `should build measurement with correct metadata`() {
        val time = Instant.parse("2026-01-01T12:00:00Z")
        val data = validData()

        val measurement = factory.create(data, loggerId = 2, groupId = 5, time = time)

        then(measurement.createdAt).isEqualTo(time)
        then(measurement.deviceId).isEqualTo(1)
        then(measurement.loggerId).isEqualTo(2)
        then(measurement.groupId).isEqualTo(5)
        then(measurement.type).isEqualTo(TemperatureMeasurementFactory.TYPE)
    }

    @Test
    fun `should normalize and map temperature fields`() {
        val data = validData(
            temperature = BigDecimal("21.211"),
            humidity = BigDecimal("55.489"),
            batteryVoltage = BigDecimal("3.1921"),
            batteryLevel = 84
        )

        val measurement = factory.create(data, loggerId = 1, groupId = 1, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["temperature"]).isEqualTo(BigDecimal("21.21"))
        then(measurement.fields["humidity"]).isEqualTo(BigDecimal("55.49"))
        then(measurement.fields["battery_voltage"]).isEqualTo(BigDecimal("3.192"))
        then(measurement.fields["battery_level"]).isEqualTo(84)
    }

    @Test
    fun `should build measurement without battery`() {
        val data = validData(batteryVoltage = null, batteryLevel = null)

        val measurement = factory.create(data, loggerId = 1, groupId = 1, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["battery_voltage"]).isNull()
        then(measurement.fields["battery_level"]).isNull()
    }

    private fun validData(
        temperature: BigDecimal = BigDecimal("21.0"),
        humidity: BigDecimal = BigDecimal("55.0"),
        batteryVoltage: BigDecimal? = BigDecimal("3.7"),
        batteryLevel: Int? = 80
    ) = TemperatureMeasurement(
        deviceId = 1,
        time = Instant.parse("2026-01-01T12:00:00Z"),
        temperature = temperature,
        humidity = humidity,
        batteryVoltage = batteryVoltage,
        batteryLevel = batteryLevel
    )
}
