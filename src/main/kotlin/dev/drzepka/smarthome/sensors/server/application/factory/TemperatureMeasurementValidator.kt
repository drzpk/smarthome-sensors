package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.TemperatureMeasurement
import java.math.BigDecimal
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class TemperatureMeasurementValidator : MeasurementValidator<TemperatureMeasurement> {

    override fun supports(input: MeasurementDto) = input is TemperatureMeasurement

    override fun validate(input: TemperatureMeasurement): ValidationResult {
        val errors = ValidationErrors()

        if (input.temperature !in MIN_TEMPERATURE..MAX_TEMPERATURE)
            errors.addFieldError(
                "temperature",
                "Temperature is out of allowed bounds: [$MIN_TEMPERATURE; $MAX_TEMPERATURE]."
            )

        if (input.humidity !in MIN_HUMIDITY..MAX_HUMIDITY)
            errors.addFieldError(
                "humidity",
                "Humidity is out of allowed bounds: [$MIN_HUMIDITY; $MAX_HUMIDITY]"
            )

        if (input.batteryLevel != null && input.batteryLevel !in MIN_BATTERY_LEVEL..MAX_BATTERY_LEVEL)
            errors.addFieldError(
                "batteryLevel",
                "Battery level is out of allowed bounds: [$MIN_BATTERY_LEVEL; $MAX_BATTERY_LEVEL]."
            )

        if (input.batteryVoltage != null && input.batteryVoltage !in MIN_BATTERY_VOLTAGE..MAX_BATTERY_VOLTAGE)
            errors.addFieldError(
                "batteryVoltage",
                "Battery voltage is out of allowed bounds: [$MIN_BATTERY_VOLTAGE; $MAX_BATTERY_VOLTAGE]."
            )

        return if (errors.errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors.errors)
    }

    companion object {
        val MIN_TEMPERATURE: BigDecimal = BigDecimal.valueOf(-30)
        val MAX_TEMPERATURE: BigDecimal = BigDecimal.valueOf(60)
        val MIN_HUMIDITY: BigDecimal = BigDecimal.valueOf(0)
        val MAX_HUMIDITY: BigDecimal = BigDecimal.valueOf(100)
        val MIN_BATTERY_VOLTAGE: BigDecimal = BigDecimal.valueOf(0)
        val MAX_BATTERY_VOLTAGE: BigDecimal = BigDecimal.valueOf(6)
        const val MIN_BATTERY_LEVEL = 0
        const val MAX_BATTERY_LEVEL = 100
    }
}
