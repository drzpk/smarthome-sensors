package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import java.time.Duration
import java.time.Instant

class MeasurementCommonValidator {

    fun validate(device: Device?, time: Instant, now: Instant): ValidationResult {
        val errors = ValidationErrors()

        if (time.isAfter(now))
            errors.addObjectError("Measurement time cannot be in the future")

        if (time.isBefore(now.minus(MAXIMUM_TIME_OFFSET)))
            errors.addObjectError("Cannot create measurements older than $MAXIMUM_TIME_OFFSET")

        if (device == null || !device.active)
            errors.addFieldError("deviceId", "Device wasn't found")

        return if (errors.errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors.errors)
    }

    companion object {
        private val MAXIMUM_TIME_OFFSET = Duration.ofHours(24)
    }
}
