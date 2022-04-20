package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

@Mockable
class MeasurementFactory(private val deviceRepository: DeviceRepository) {

    /**
     * Builds measurement entity from DTO.
     *
     * Server time is used to calculate measurement time, because it's easier
     * than ensuring that all loggers' time and zone are configured correctly.
     * High time precision is not required.
     */
    fun create(input: CreateMeasurementsRequest.Measurement, loggerId: Int, now: Instant = Instant.now()): Measurement {
        val device = deviceRepository.findById(input.deviceId)
        validate(input, device, now)
        return doBuild(input, now, loggerId, device?.group?.id!!)
    }

    private fun validate(input: CreateMeasurementsRequest.Measurement, device: Device?, now: Instant) {
        val validation = ValidationErrors()

        if (input.temperature < MIN_ALLOWED_TEMPERATURE || input.temperature > MAX_ALLOWED_TEMPERATURE)
            validation.addFieldError(
                "temperature",
                "Temperature is out of allowed bounds: [$MIN_ALLOWED_TEMPERATURE; $MAX_ALLOWED_TEMPERATURE]."
            )

        if (input.humidity < MIN_ALLOWED_HUMIDITY || input.humidity > MAX_ALLOWED_HUMIDITY)
            validation.addFieldError(
                "humidity",
                "Humidity is out of allowed bounds: [$MIN_ALLOWED_HUMIDITY; $MAX_ALLOWED_HUMIDITY]"
            )

        if (input.batteryLevel != null && (input.batteryLevel!! < MIN_ALLOWED_BATTERY_LEVEL || input.batteryLevel!! > MAX_ALLOWED_BATTERY_LEVEL))
            validation.addFieldError(
                "batteryLevel",
                "Battery level is out of allowed bounds: [$MIN_ALLOWED_BATTERY_LEVEL; $MAX_ALLOWED_BATTERY_LEVEL]."
            )

        if (input.batteryVoltage != null && (input.batteryVoltage!! < MIN_ALLOWED_BATTERY_VOLTAGE || input.batteryVoltage!! > MAX_ALLOWED_BATTERY_VOLTAGE))
            validation.addFieldError(
                "batteryVoltage",
                "Battery voltage is out of allowed bounds: [$MIN_ALLOWED_BATTERY_VOLTAGE; $MAX_ALLOWED_BATTERY_VOLTAGE]."
            )

        if (input.timestampOffsetMillis < 0)
            validation.addFieldError(
                "timestampOffsetMillis",
                "Timestamp offset must be equal or greater than 0"
            )

        if (input.timestampOffsetMillis >= 0
            && getAbsoluteTime(now, input.timestampOffsetMillis) < now.minus(MAXIMUM_TIME_OFFSET)
        ) {
            validation.addObjectError("Cannot create measurements older than $MAXIMUM_TIME_OFFSET")
        }

        if (device == null || !device.active)
            validation.addFieldError("deviceId", "Device wasn't found")

        validation.verify()
    }

    private fun doBuild(
        input: CreateMeasurementsRequest.Measurement,
        now: Instant,
        loggerId: Int,
        groupId: Int
    ): Measurement {
        val measurement = Measurement(
            getAbsoluteTime(now, input.timestampOffsetMillis),
            input.deviceId,
            loggerId,
            groupId
        )

        return measurement.apply {
            temperature = normalizeNumber(input.temperature, 2)
            humidity = normalizeNumber(input.humidity, 2)
            batteryVoltage = input.batteryVoltage?.let { normalizeNumber(it, 3) }
            batteryLevel = input.batteryLevel
        }
    }

    private fun getAbsoluteTime(now: Instant, offsetMillis: Long): Instant = now.minusMillis(offsetMillis)

    private fun normalizeNumber(input: BigDecimal, scale: Int): BigDecimal = input.setScale(scale, RoundingMode.HALF_UP)

    companion object {
        private val MIN_ALLOWED_TEMPERATURE = BigDecimal.valueOf(-30)
        private val MAX_ALLOWED_TEMPERATURE = BigDecimal.valueOf(60)
        private val MIN_ALLOWED_HUMIDITY = BigDecimal.valueOf(0)
        private val MAX_ALLOWED_HUMIDITY = BigDecimal.valueOf(100)
        private val MIN_ALLOWED_BATTERY_VOLTAGE = BigDecimal.valueOf(0)
        private val MAX_ALLOWED_BATTERY_VOLTAGE = BigDecimal.valueOf(6)
        private const val MIN_ALLOWED_BATTERY_LEVEL = 0
        private const val MAX_ALLOWED_BATTERY_LEVEL = 100

        private val MAXIMUM_TIME_OFFSET = Duration.ofHours(24)
    }
}