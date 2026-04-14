package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.CreateMeasurementsRequestV2
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureData
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

    fun create(input: CreateMeasurementsRequestV2.Measurement, loggerId: Int, now: Instant = Instant.now()): Measurement {
        val device = deviceRepository.findById(input.deviceId)
        validateCommon(input, device, now)

        val (type, fields) = when (val data = input.data) {
            is TemperatureData -> "temperature" to buildTemperatureFields(data)
        }

        return Measurement(
            createdAt = getAbsoluteTime(now, input.timestampOffsetMillis),
            deviceId = input.deviceId,
            loggerId = loggerId,
            groupId = device?.group?.id!!,
            type = type,
            fields = fields
        )
    }

    private fun validateCommon(input: CreateMeasurementsRequestV2.Measurement, device: Device?, now: Instant) {
        val validation = ValidationErrors()

        if (input.timestampOffsetMillis < 0)
            validation.addFieldError("timestampOffsetMillis", "Timestamp offset must be equal or greater than 0")

        if (input.timestampOffsetMillis >= 0
            && getAbsoluteTime(now, input.timestampOffsetMillis) < now.minus(MAXIMUM_TIME_OFFSET)
        ) {
            validation.addObjectError("Cannot create measurements older than $MAXIMUM_TIME_OFFSET")
        }

        if (device == null || !device.active)
            validation.addFieldError("deviceId", "Device wasn't found")

        validation.verify()
    }

    private fun buildTemperatureFields(data: TemperatureData): Map<String, Number?> {
        val validation = ValidationErrors()

        if (data.temperature !in MIN_TEMPERATURE..MAX_TEMPERATURE)
            validation.addFieldError(
                "temperature",
                "Temperature is out of allowed bounds: [$MIN_TEMPERATURE; $MAX_TEMPERATURE]."
            )

        if (data.humidity !in MIN_HUMIDITY..MAX_HUMIDITY)
            validation.addFieldError(
                "humidity",
                "Humidity is out of allowed bounds: [$MIN_HUMIDITY; $MAX_HUMIDITY]"
            )

        if (data.batteryLevel != null && (data.batteryLevel !in MIN_BATTERY_LEVEL..MAX_BATTERY_LEVEL))
            validation.addFieldError(
                "batteryLevel",
                "Battery level is out of allowed bounds: [$MIN_BATTERY_LEVEL; $MAX_BATTERY_LEVEL]."
            )

        if (data.batteryVoltage != null && (data.batteryVoltage !in MIN_BATTERY_VOLTAGE..MAX_BATTERY_VOLTAGE))
            validation.addFieldError(
                "batteryVoltage",
                "Battery voltage is out of allowed bounds: [$MIN_BATTERY_VOLTAGE; $MAX_BATTERY_VOLTAGE]."
            )

        validation.verify()

        return mapOf(
            "temperature" to normalizeNumber(data.temperature, 2),
            "humidity" to normalizeNumber(data.humidity, 2),
            "battery_voltage" to data.batteryVoltage?.let { normalizeNumber(it, 3) },
            "battery_level" to data.batteryLevel
        )
    }

    private fun getAbsoluteTime(now: Instant, offsetMillis: Long): Instant = now.minusMillis(offsetMillis)

    private fun normalizeNumber(input: BigDecimal, scale: Int): BigDecimal = input.setScale(scale, RoundingMode.HALF_UP)

    companion object {
        private val MIN_TEMPERATURE = BigDecimal.valueOf(-30)
        private val MAX_TEMPERATURE = BigDecimal.valueOf(60)
        private val MIN_HUMIDITY = BigDecimal.valueOf(0)
        private val MAX_HUMIDITY = BigDecimal.valueOf(100)
        private val MIN_BATTERY_VOLTAGE = BigDecimal.valueOf(0)
        private val MAX_BATTERY_VOLTAGE = BigDecimal.valueOf(6)
        private const val MIN_BATTERY_LEVEL = 0
        private const val MAX_BATTERY_LEVEL = 100
        private val MAXIMUM_TIME_OFFSET = Duration.ofHours(24)
    }
}
