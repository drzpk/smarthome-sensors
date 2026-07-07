package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyPhase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class EnergyMeasurementValidator : MeasurementValidator<EnergyMeasurement> {

    override fun supports(input: MeasurementDto) = input is EnergyMeasurement

    override fun validate(input: EnergyMeasurement): ValidationResult {
        val errors = ValidationErrors()

        validatePhase("phaseA", input.phaseA, errors)
        validatePhase("phaseB", input.phaseB, errors)
        validatePhase("phaseC", input.phaseC, errors)

        return if (errors.errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors.errors)
    }

    private fun validatePhase(prefix: String, phase: EnergyPhase, errors: ValidationErrors) {
        if (phase.totalActiveEnergy < 0)
            errors.addFieldError("${prefix}.totalActiveEnergy", "Total active energy must be non-negative")
        if (phase.totalActiveReturnedEnergy < 0)
            errors.addFieldError("${prefix}.totalActiveReturnedEnergy", "Total active returned energy must be non-negative")
        if (phase.maxActivePower < 0)
            errors.addFieldError("${prefix}.maxActivePower", "Max active power must be non-negative")
        if (phase.minActivePower < 0)
            errors.addFieldError("${prefix}.minActivePower", "Min active power must be non-negative")
        if (phase.maxApparentPower < 0)
            errors.addFieldError("${prefix}.maxApparentPower", "Max apparent power must be non-negative")
        if (phase.minApparentPower < 0)
            errors.addFieldError("${prefix}.minApparentPower", "Min apparent power must be non-negative")
        if (phase.maxVoltage < 0)
            errors.addFieldError("${prefix}.maxVoltage", "Max voltage must be non-negative")
        if (phase.minVoltage < 0)
            errors.addFieldError("${prefix}.minVoltage", "Min voltage must be non-negative")
        if (phase.maxCurrent < 0)
            errors.addFieldError("${prefix}.maxCurrent", "Max current must be non-negative")
        if (phase.minCurrent < 0)
            errors.addFieldError("${prefix}.minCurrent", "Min current must be non-negative")
        if (phase.fundamentalActiveEnergy < 0)
            errors.addFieldError("${prefix}.fundamentalActiveEnergy", "Fundamental active energy must be non-negative")
        if (phase.fundamentalActiveReturnedEnergy < 0)
            errors.addFieldError("${prefix}.fundamentalActiveReturnedEnergy", "Fundamental active returned energy must be non-negative")
        if (phase.laggingReactiveEnergy < 0)
            errors.addFieldError("${prefix}.laggingReactiveEnergy", "Lagging reactive energy must be non-negative")
        if (phase.leadingReactiveEnergy < 0)
            errors.addFieldError("${prefix}.leadingReactiveEnergy", "Leading reactive energy must be non-negative")
    }
}
