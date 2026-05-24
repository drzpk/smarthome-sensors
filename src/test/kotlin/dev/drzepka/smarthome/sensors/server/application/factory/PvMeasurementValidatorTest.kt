package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Pv
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.PvMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.TemperatureMeasurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PvMeasurementValidatorTest {

    private val validator = PvMeasurementValidator()

    @Test
    fun `should support PvMeasurement`() {
        then(validator.supports(validData())).isTrue()
    }

    @Test
    fun `should not support other measurement types`() {
        val temperature = TemperatureMeasurement(deviceId = 1, time = null)
        then(validator.supports(temperature)).isFalse()
    }

    @Test
    fun `should pass valid measurement`() {
        then(validator.validate(validData())).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should pass measurement with null PV panels`() {
        val data = validData(pv1 = null, pv2 = null)

        then(validator.validate(data)).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should reject negative total power`() {
        val data = validData(totalPower = -1)

        assertFieldError("totalPower", validator.validate(data))
    }

    @Test
    fun `should reject negative energy today`() {
        val data = validData(energyToday = BigDecimal("-0.1"))

        assertFieldError("energyToday", validator.validate(data))
    }

    @Test
    fun `should reject negative energy total`() {
        val data = validData(energyTotal = BigDecimal("-0.1"))

        assertFieldError("energyTotal", validator.validate(data))
    }

    private fun validData(
        totalPower: Int = 3000,
        energyToday: BigDecimal = BigDecimal("12.5"),
        energyTotal: BigDecimal = BigDecimal("1500.0"),
        pv1: Pv? = validPv(),
        pv2: Pv? = validPv()
    ) = PvMeasurement(
        deviceId = 1,
        time = Instant.parse("2026-01-01T12:00:00Z"),
        totalPower = totalPower,
        energyToday = energyToday,
        energyTotal = energyTotal,
        phaseA = validPhase(),
        phaseB = validPhase(),
        phaseC = validPhase(),
        pv1 = pv1,
        pv2 = pv2
    )

    private fun validPhase() = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f)

    private fun validPv() = Pv(voltage = 350.0f, current = 4.3f, power = 1500, energyToday = BigDecimal("6.2"))

    private fun assertFieldError(fieldName: String, result: ValidationResult) {
        then(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val hasFieldError = (result as ValidationResult.Invalid).errors
            .any { it is FieldError && it.field == fieldName }
        then(hasFieldError).withFailMessage("Field error '$fieldName' wasn't found").isTrue()
    }
}
