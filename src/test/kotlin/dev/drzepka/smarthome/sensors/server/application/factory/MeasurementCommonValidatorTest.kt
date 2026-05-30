package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ObjectError
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class MeasurementCommonValidatorTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val activeDevice = Device(Group().apply { id = 1 }).apply { active = true }
    private val validator = MeasurementCommonValidator()

    @Test
    fun `should pass with valid device and time`() {
        then(validator.validate(activeDevice, now, now)).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should reject inactive device`() {
        val inactiveDevice = Device(Group().apply { id = 1 }).apply { active = false }

        assertFieldError("mac", validator.validate(inactiveDevice, now, now))
    }

    @Test
    fun `should reject time in the future`() {
        val futureTime = now.plusSeconds(1)

        assertObjectError("future", validator.validate(activeDevice, futureTime, now))
    }

    @Test
    fun `should accept time equal to now`() {
        then(validator.validate(activeDevice, now, now)).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `should reject time older than maximum offset`() {
        val oldTime = now.minus(Duration.ofHours(25))

        assertObjectError("older than", validator.validate(activeDevice, oldTime, now))
    }

    @Test
    fun `should accept time exactly at maximum offset boundary`() {
        val boundaryTime = now.minus(Duration.ofHours(24))

        then(validator.validate(activeDevice, boundaryTime, now)).isEqualTo(ValidationResult.Valid)
    }

    private fun assertFieldError(fieldName: String, result: ValidationResult) {
        then(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val hasFieldError = (result as ValidationResult.Invalid).errors
            .any { it is FieldError && it.field == fieldName }
        then(hasFieldError).withFailMessage("Field error '$fieldName' wasn't found").isTrue()
    }

    private fun assertObjectError(messageContains: String, result: ValidationResult) {
        then(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val hasObjectError = (result as ValidationResult.Invalid).errors
            .any { it is ObjectError && it.message.contains(messageContains) }
        then(hasObjectError).withFailMessage("Object error containing '$messageContains' wasn't found").isTrue()
    }
}
