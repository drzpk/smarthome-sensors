package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureMeasurement
import dev.drzepka.smarthome.sensors.server.application.util.mapOfNotNull
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Measurement as MeasurementDto

class TemperatureMeasurementFactory : MeasurementFactory<TemperatureMeasurement> {

    override fun supports(input: MeasurementDto) = input is TemperatureMeasurement

    override fun create(input: TemperatureMeasurement, loggerId: Int, groupId: Int, time: Instant): Measurement {
        return Measurement(
            createdAt = time,
            deviceId = input.deviceId,
            loggerId = loggerId,
            groupId = groupId,
            type = TYPE,
            fields = buildFields(input)
        )
    }

    private fun buildFields(data: TemperatureMeasurement): Map<String, Number> = mapOfNotNull(
        "temperature" to normalizeNumber(data.temperature, 2),
        "humidity" to normalizeNumber(data.humidity, 2),
        "battery_voltage" to data.batteryVoltage?.let { normalizeNumber(it, 3) },
        "battery_level" to data.batteryLevel
    )

    private fun normalizeNumber(input: BigDecimal, scale: Int): BigDecimal = input.setScale(scale, RoundingMode.HALF_UP)

    companion object {
        const val TYPE = "temperature"
    }
}
