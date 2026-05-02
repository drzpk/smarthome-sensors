package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.PvMeasurement
import java.math.BigDecimal
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Measurement as MeasurementDto

class PvMeasurementValidator : MeasurementValidator<PvMeasurement> {

    override fun supports(input: MeasurementDto) = input is PvMeasurement

    override fun validate(input: PvMeasurement): ValidationResult {
        val errors = ValidationErrors()

        if (input.totalPower < 0)
            errors.addFieldError("totalPower", "Total power must be non-negative")

        if (input.energyToday < BigDecimal.ZERO)
            errors.addFieldError("energyToday", "Energy today must be non-negative")

        if (input.energyTotal < BigDecimal.ZERO)
            errors.addFieldError("energyTotal", "Energy total must be non-negative")

        return if (errors.errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors.errors)
    }
}
