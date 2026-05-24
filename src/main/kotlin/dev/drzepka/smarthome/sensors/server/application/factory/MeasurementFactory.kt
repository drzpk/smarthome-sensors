package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

interface MeasurementFactory<T : MeasurementDto> {
    fun supports(input: MeasurementDto): Boolean
    fun create(input: T, loggerId: Int, groupId: Int, time: Instant): Measurement

    @Suppress("UNCHECKED_CAST")
    fun createUnchecked(input: MeasurementDto, loggerId: Int, groupId: Int, time: Instant): Measurement =
        create(input as T, loggerId, groupId, time)
}
