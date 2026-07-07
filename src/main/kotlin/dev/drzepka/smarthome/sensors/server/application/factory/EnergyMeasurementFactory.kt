package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyPhase
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.util.asDouble
import dev.drzepka.smarthome.sensors.server.domain.util.asFloat
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class EnergyMeasurementFactory : MeasurementFactory<EnergyMeasurement> {

    override fun supports(input: MeasurementDto) = input is EnergyMeasurement

    override fun create(input: EnergyMeasurement, loggerId: Int, device: Device, time: Instant): Measurement {
        return Measurement(
            createdAt = time,
            deviceId = device.id!!,
            loggerId = loggerId,
            groupId = device.group?.id!!,
            type = TYPE,
            fields = buildFields(input),
            liveFields = buildLiveFields(input)
        )
    }

    private fun buildFields(data: EnergyMeasurement): Map<String, Number> {
        val fields = mutableMapOf<String, Number>()
        fields += phaseFields("phase_a", data.phaseA)
        fields += phaseFields("phase_b", data.phaseB)
        fields += phaseFields("phase_c", data.phaseC)
        return fields
    }

    private fun buildLiveFields(data: EnergyMeasurement): Map<String, Number> {
        val fields = mutableMapOf<String, Number>()
        fields += phaseLiveFields("phase_a", data.phaseA)
        fields += phaseLiveFields("phase_b", data.phaseB)
        fields += phaseLiveFields("phase_c", data.phaseC)
        return fields
    }

    private fun phaseFields(prefix: String, phase: EnergyPhase): Map<String, Number> = mapOf(
        "${prefix}_total_active_energy" to phase.totalActiveEnergy.asDouble(),
        "${prefix}_total_active_returned_energy" to phase.totalActiveReturnedEnergy.asDouble(),
        "${prefix}_max_active_power" to phase.maxActivePower.asFloat(),
        "${prefix}_min_active_power" to phase.minActivePower.asFloat(),
        "${prefix}_max_apparent_power" to phase.maxApparentPower.asFloat(),
        "${prefix}_min_apparent_power" to phase.minApparentPower.asFloat(),
        "${prefix}_max_voltage" to phase.maxVoltage.asFloat(),
        "${prefix}_min_voltage" to phase.minVoltage.asFloat(),
        "${prefix}_max_current" to phase.maxCurrent.asFloat(),
        "${prefix}_min_current" to phase.minCurrent.asFloat(),
    )

    private fun phaseLiveFields(prefix: String, phase: EnergyPhase): Map<String, Number> = mapOf(
        "${prefix}_fundamental_active_energy" to phase.fundamentalActiveEnergy.asFloat(),
        "${prefix}_fundamental_active_returned_energy" to phase.fundamentalActiveReturnedEnergy.asFloat(),
        "${prefix}_lagging_reactive_energy" to phase.laggingReactiveEnergy.asFloat(),
        "${prefix}_leading_reactive_energy" to phase.leadingReactiveEnergy.asFloat(),
    )

    companion object {
        const val TYPE = "energy"
    }
}
