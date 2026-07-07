package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyPhase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.TemperatureMeasurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Instant

class EnergyMeasurementValidatorTest {

    private val validator = EnergyMeasurementValidator()

    @Test
    fun `should support EnergyMeasurement`() {
        then(validator.supports(validData())).isTrue()
    }

    @Test
    fun `should not support other measurement types`() {
        val temperature = TemperatureMeasurement(mac = "mac", time = null)
        then(validator.supports(temperature)).isFalse()
    }

    @Test
    fun `should pass valid measurement`() {
        then(validator.validate(validData())).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should reject negative total active energy in phase A`() {
        val data = validData(phaseA = validPhase().copy(totalActiveEnergy = -0.1))

        assertFieldError("phaseA.totalActiveEnergy", validator.validate(data))
    }

    @Test
    fun `should reject negative total active returned energy in phase C`() {
        val data = validData(phaseC = validPhase().copy(totalActiveReturnedEnergy = -0.1))

        assertFieldError("phaseC.totalActiveReturnedEnergy", validator.validate(data))
    }

    @Test
    fun `should reject negative max active power`() {
        val data = validData(phaseA = validPhase().copy(maxActivePower = -0.1f))

        assertFieldError("phaseA.maxActivePower", validator.validate(data))
    }

    @Test
    fun `should reject negative min active power`() {
        val data = validData(phaseB = validPhase().copy(minActivePower = -0.1f))

        assertFieldError("phaseB.minActivePower", validator.validate(data))
    }

    @Test
    fun `should reject negative max apparent power`() {
        val data = validData(phaseA = validPhase().copy(maxApparentPower = -0.1f))

        assertFieldError("phaseA.maxApparentPower", validator.validate(data))
    }

    @Test
    fun `should reject negative min apparent power`() {
        val data = validData(phaseB = validPhase().copy(minApparentPower = -0.1f))

        assertFieldError("phaseB.minApparentPower", validator.validate(data))
    }

    @Test
    fun `should reject negative max voltage`() {
        val data = validData(phaseA = validPhase().copy(maxVoltage = -0.1f))

        assertFieldError("phaseA.maxVoltage", validator.validate(data))
    }

    @Test
    fun `should reject negative min voltage`() {
        val data = validData(phaseB = validPhase().copy(minVoltage = -0.1f))

        assertFieldError("phaseB.minVoltage", validator.validate(data))
    }

    @Test
    fun `should reject negative max current`() {
        val data = validData(phaseA = validPhase().copy(maxCurrent = -0.1f))

        assertFieldError("phaseA.maxCurrent", validator.validate(data))
    }

    @Test
    fun `should reject negative min current`() {
        val data = validData(phaseB = validPhase().copy(minCurrent = -0.1f))

        assertFieldError("phaseB.minCurrent", validator.validate(data))
    }

    @Test
    fun `should reject negative fundamental active energy in phase B`() {
        val data = validData(phaseB = validPhase().copy(fundamentalActiveEnergy = -0.1f))

        assertFieldError("phaseB.fundamentalActiveEnergy", validator.validate(data))
    }

    @Test
    fun `should reject negative fundamental active returned energy`() {
        val data = validData(phaseA = validPhase().copy(fundamentalActiveReturnedEnergy = -0.1f))

        assertFieldError("phaseA.fundamentalActiveReturnedEnergy", validator.validate(data))
    }

    @Test
    fun `should reject negative lagging reactive energy`() {
        val data = validData(phaseA = validPhase().copy(laggingReactiveEnergy = -0.1f))

        assertFieldError("phaseA.laggingReactiveEnergy", validator.validate(data))
    }

    @Test
    fun `should reject negative leading reactive energy`() {
        val data = validData(phaseA = validPhase().copy(leadingReactiveEnergy = -0.1f))

        assertFieldError("phaseA.leadingReactiveEnergy", validator.validate(data))
    }

    private fun validData(
        phaseA: EnergyPhase = validPhase(),
        phaseB: EnergyPhase = validPhase(),
        phaseC: EnergyPhase = validPhase()
    ) = EnergyMeasurement(
        mac = "mac",
        time = Instant.parse("2026-01-01T12:00:00Z"),
        phaseA = phaseA,
        phaseB = phaseB,
        phaseC = phaseC
    )

    private fun validPhase() = EnergyPhase(
        totalActiveEnergy = 100.0,
        totalActiveReturnedEnergy = 10.0,
        maxActivePower = 500.0f,
        minActivePower = 100.0f,
        maxApparentPower = 550.0f,
        minApparentPower = 110.0f,
        maxVoltage = 235.0f,
        minVoltage = 225.0f,
        maxCurrent = 5.0f,
        minCurrent = 1.0f,
        fundamentalActiveEnergy = 90.0f,
        fundamentalActiveReturnedEnergy = 8.0f,
        laggingReactiveEnergy = 5.0f,
        leadingReactiveEnergy = 3.0f
    )

    private fun assertFieldError(fieldName: String, result: ValidationResult) {
        then(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val hasFieldError = (result as ValidationResult.Invalid).errors
            .any { it is FieldError && it.field == fieldName }
        then(hasFieldError).withFailMessage("Field error '$fieldName' wasn't found").isTrue()
    }
}
