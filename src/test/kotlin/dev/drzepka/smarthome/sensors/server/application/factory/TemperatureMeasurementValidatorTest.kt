package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.PvMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureMeasurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TemperatureMeasurementValidatorTest {

    private val validator = TemperatureMeasurementValidator()

    @Test
    fun `should support TemperatureMeasurement`() {
        then(validator.supports(validData())).isTrue()
    }

    @Test
    fun `should not support other measurement types`() {
        then(validator.supports(validPvData())).isFalse()
    }

    @Test
    fun `should pass valid measurement`() {
        then(validator.validate(validData())).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should pass measurement without battery`() {
        val data = validData(batteryVoltage = null, batteryLevel = null)

        then(validator.validate(data)).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should reject temperature below minimum`() {
        val data = validData(temperature = TemperatureMeasurementValidator.MIN_TEMPERATURE - BigDecimal.ONE)

        assertFieldError("temperature", validator.validate(data))
    }

    @Test
    fun `should reject temperature above maximum`() {
        val data = validData(temperature = TemperatureMeasurementValidator.MAX_TEMPERATURE + BigDecimal.ONE)

        assertFieldError("temperature", validator.validate(data))
    }

    @Test
    fun `should reject humidity below minimum`() {
        val data = validData(humidity = TemperatureMeasurementValidator.MIN_HUMIDITY - BigDecimal.ONE)

        assertFieldError("humidity", validator.validate(data))
    }

    @Test
    fun `should reject humidity above maximum`() {
        val data = validData(humidity = TemperatureMeasurementValidator.MAX_HUMIDITY + BigDecimal.ONE)

        assertFieldError("humidity", validator.validate(data))
    }

    @Test
    fun `should reject battery level below minimum`() {
        val data = validData(batteryLevel = TemperatureMeasurementValidator.MIN_BATTERY_LEVEL - 1)

        assertFieldError("batteryLevel", validator.validate(data))
    }

    @Test
    fun `should reject battery level above maximum`() {
        val data = validData(batteryLevel = TemperatureMeasurementValidator.MAX_BATTERY_LEVEL + 1)

        assertFieldError("batteryLevel", validator.validate(data))
    }

    @Test
    fun `should reject battery voltage below minimum`() {
        val data = validData(batteryVoltage = TemperatureMeasurementValidator.MIN_BATTERY_VOLTAGE - BigDecimal.ONE)

        assertFieldError("batteryVoltage", validator.validate(data))
    }

    @Test
    fun `should reject battery voltage above maximum`() {
        val data = validData(batteryVoltage = TemperatureMeasurementValidator.MAX_BATTERY_VOLTAGE + BigDecimal.ONE)

        assertFieldError("batteryVoltage", validator.validate(data))
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

    private fun validPvData() = PvMeasurement(
        deviceId = 1,
        time = Instant.parse("2026-01-01T12:00:00Z"),
        totalPower = 3000,
        energyToday = BigDecimal("12.5"),
        energyTotal = BigDecimal("1500.0"),
        phaseA = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseB = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseC = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        pv1 = null,
        pv2 = null
    )

    private fun assertFieldError(fieldName: String, result: ValidationResult) {
        then(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val hasFieldError = (result as ValidationResult.Invalid).errors
            .any { it is FieldError && it.field == fieldName }
        then(hasFieldError).withFailMessage("Field error '$fieldName' wasn't found").isTrue()
    }
}
