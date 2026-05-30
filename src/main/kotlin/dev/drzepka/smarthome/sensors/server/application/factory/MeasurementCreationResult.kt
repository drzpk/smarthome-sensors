package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationError
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement

sealed interface MeasurementCreationResult {
    data class Success(val measurement: Measurement) : MeasurementCreationResult
    data class UnknownDevice(val mac: String) : MeasurementCreationResult
    data class ValidationFailed(val errors: List<ValidationError>) : MeasurementCreationResult
}
