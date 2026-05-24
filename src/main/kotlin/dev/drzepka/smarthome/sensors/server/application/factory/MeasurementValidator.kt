package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

interface MeasurementValidator<T : MeasurementDto> {
    fun supports(input: MeasurementDto): Boolean
    fun validate(input: T): ValidationResult

    @Suppress("UNCHECKED_CAST")
    fun validateUnchecked(input: MeasurementDto): ValidationResult = validate(input as T)
}
