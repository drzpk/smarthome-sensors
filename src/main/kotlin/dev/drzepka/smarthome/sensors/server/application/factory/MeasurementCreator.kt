package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

@Mockable
class MeasurementCreator(
    private val deviceRepository: DeviceRepository,
    private val commonValidator: MeasurementCommonValidator,
    private val validators: List<MeasurementValidator<*>>,
    private val factories: List<MeasurementFactory<*>>
) {

    fun create(input: MeasurementDto, loggerId: Int, now: Instant = Instant.now()): MeasurementCreationResult {
        val device = deviceRepository.findByMac(input.mac)
            ?: return MeasurementCreationResult.UnknownDevice(input.mac)

        val time = input.time ?: now

        val commonResult = commonValidator.validate(device, time, now)
        if (commonResult is ValidationResult.Invalid)
            return MeasurementCreationResult.ValidationFailed(commonResult.errors)

        val specificResult = validators.firstOrNull { it.supports(input) }?.validateUnchecked(input)
        if (specificResult is ValidationResult.Invalid)
            return MeasurementCreationResult.ValidationFailed(specificResult.errors)

        val factory = factories.firstOrNull { it.supports(input) }
            ?: error("No factory found for measurement type: ${input::class.simpleName}")

        return MeasurementCreationResult.Success(factory.createUnchecked(input, loggerId, device, time))
    }
}
