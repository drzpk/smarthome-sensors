package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import java.math.BigDecimal

class MeasurementService(
    configurationProviderService: ConfigurationProviderService,
    private val deviceRepository: DeviceRepository
) {
    private val log by Logger()
    private val minimumCreationInterval =
        configurationProviderService.getInt("measurements.minimumCreationIntervalSeconds")

    fun addMeasurement(
        request: CreateMeasurementRequest,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): Boolean {
        return true
    }

    private fun validateAddMeasurement(request: CreateMeasurementRequest): Device {
        val validation = ValidationErrors()

        if (request.temperature < MIN_ALLOWED_TEMPERATURE || request.temperature > MAX_ALLOWED_TEMPERATURE)
            validation.addFieldError("temperature", "Temperature is out of allowed bounds: [-30; 60].")
        if (request.humidity < BigDecimal.ZERO || request.humidity > HUNDRED)
            validation.addFieldError("humidity", "Humidity is out of allowed bounds: [0; 100]")
        if (request.batteryLevel < MIN_ALLOWED_BATTERY_LEVEL || request.batteryLevel > MAX_ALLOWED_BATTERY_LEVEL)
            validation.addFieldError("batteryLevel", "Battery level is out of allowed bounds: [0; 100].")
        if (request.batteryVoltage < MIN_ALLOWED_BATTERY_VOLTAGE || request.batteryVoltage > MAX_ALLOWED_BATTERY_VOLTAGE)
            validation.addFieldError("batteryVoltage", "Battery voltage is out of allowed bounds: [0; 10].")

        val device = deviceRepository.findById(request.deviceId)
        if (device == null || !device.active)
            validation.addFieldError("deviceId", "Device wasn't found")

        validation.verify()

        return device!!
    }

    private fun checkMeasurementNotAddedBeforeInterval() {

    }

    companion object {
        private val MIN_ALLOWED_TEMPERATURE = BigDecimal.valueOf(-30)
        private val MAX_ALLOWED_TEMPERATURE = BigDecimal.valueOf(60)
        private val MIN_ALLOWED_BATTERY_VOLTAGE = BigDecimal.valueOf(0)
        private val MAX_ALLOWED_BATTERY_VOLTAGE = BigDecimal.valueOf(10)
        private const val MIN_ALLOWED_BATTERY_LEVEL = 0
        private const val MAX_ALLOWED_BATTERY_LEVEL = 100

        private val HUNDRED = BigDecimal.valueOf(100L)
    }

}