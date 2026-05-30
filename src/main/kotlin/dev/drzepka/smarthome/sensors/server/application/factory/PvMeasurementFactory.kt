package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Pv
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.PvMeasurement
import dev.drzepka.smarthome.sensors.server.application.util.mapOfNotNull
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class PvMeasurementFactory : MeasurementFactory<PvMeasurement> {

    override fun supports(input: MeasurementDto) = input is PvMeasurement

    override fun create(input: PvMeasurement, loggerId: Int, device: Device, time: Instant): Measurement {
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

    private fun buildLiveFields(data: PvMeasurement): Map<String, Number> = mapOf(
        "energy_today" to data.energyToday
    )

    private fun buildFields(data: PvMeasurement): Map<String, Number> {
        val fields = mutableMapOf<String, Number>(
            "total_power" to data.totalPower,
            "energy_total" to data.energyTotal
        )
        fields += phaseFields("phase_a", data.phaseA)
        fields += phaseFields("phase_b", data.phaseB)
        fields += phaseFields("phase_c", data.phaseC)
        fields += pvPanelFields("pv1", data.pv1)
        fields += pvPanelFields("pv2", data.pv2)
        return fields
    }

    private fun phaseFields(prefix: String, phase: Phase?): Map<String, Number> = if (phase != null)
        mapOfNotNull(
            "${prefix}_voltage" to phase.voltage,
            "${prefix}_current" to phase.current,
            "${prefix}_power" to phase.power,
            "${prefix}_frequency" to phase.frequency
        )
    else emptyMap()

    private fun pvPanelFields(prefix: String, pv: Pv?): Map<String, Number> = if (pv != null)
        mapOf(
            "${prefix}_voltage" to pv.voltage,
            "${prefix}_current" to pv.current,
            "${prefix}_power" to pv.power,
            "${prefix}_energy_today" to pv.energyToday
        )
    else emptyMap()

    companion object {
        const val TYPE = "pv"
    }
}
