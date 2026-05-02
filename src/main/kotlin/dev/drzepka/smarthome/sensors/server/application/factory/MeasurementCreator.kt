package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Measurement as MeasurementDto

@Mockable
class MeasurementCreator(
    private val deviceRepository: DeviceRepository,
    private val commonValidator: MeasurementCommonValidator,
    private val validators: List<MeasurementValidator<*>>,
    private val factories: List<MeasurementFactory<*>>
) {

    fun create(input: MeasurementDto, loggerId: Int, now: Instant = Instant.now()): Measurement {
        val device = deviceRepository.findById(input.deviceId)
        val time = input.time ?: now

        commonValidator.validate(device, time, now).throwIfInvalid()
        validators.firstOrNull { it.supports(input) }?.validateUnchecked(input)?.throwIfInvalid()

        val factory = factories.firstOrNull { it.supports(input) }
            ?: error("No factory found for measurement type: ${input::class.simpleName}")

        return factory.createUnchecked(input, loggerId, device!!.group?.id!!, time)
    }
}
