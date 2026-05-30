package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

interface MeasurementFactory<T : MeasurementDto> {
    fun supports(input: MeasurementDto): Boolean
    fun create(input: T, loggerId: Int, device: Device, time: Instant): Measurement

    @Suppress("UNCHECKED_CAST")
    fun createUnchecked(input: MeasurementDto, loggerId: Int, device: Device, time: Instant): Measurement =
        create(input as T, loggerId, device, time)
}
