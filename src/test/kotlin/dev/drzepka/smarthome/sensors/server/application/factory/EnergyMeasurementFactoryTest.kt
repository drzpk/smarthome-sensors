package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.EnergyPhase
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Instant

class EnergyMeasurementFactoryTest {

    private val factory = EnergyMeasurementFactory()
    private val device = Device(Group().apply { id = 5 }).apply { id = 1 }

    @Test
    fun `should build measurement with correct metadata`() {
        val time = Instant.parse("2026-01-01T12:00:00Z")

        val measurement = factory.create(validData(), loggerId = 2, device = device, time = time)

        then(measurement.createdAt).isEqualTo(time)
        then(measurement.deviceId).isEqualTo(1)
        then(measurement.loggerId).isEqualTo(2)
        then(measurement.groupId).isEqualTo(5)
        then(measurement.type).isEqualTo(EnergyMeasurementFactory.TYPE)
    }

    @Test
    fun `should map all energy phase fields`() {
        val phaseA = EnergyPhase(
            totalActiveEnergy = 1.0,
            totalActiveReturnedEnergy = 2.0,
            maxActivePower = 3.0f,
            minActivePower = 4.0f,
            maxApparentPower = 5.0f,
            minApparentPower = 6.0f,
            maxVoltage = 7.0f,
            minVoltage = 8.0f,
            maxCurrent = 9.0f,
            minCurrent = 10.0f,
            fundamentalActiveEnergy = 11.0f,
            fundamentalActiveReturnedEnergy = 12.0f,
            laggingReactiveEnergy = 13.0f,
            leadingReactiveEnergy = 14.0f
        )

        val measurement = factory.create(validData(phaseA = phaseA), loggerId = 1, device = device, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["phase_a_total_active_energy"]).isEqualTo(1.0)
        then(measurement.fields["phase_a_total_active_returned_energy"]).isEqualTo(2.0)
        then(measurement.fields["phase_a_max_active_power"]).isEqualTo(3.0f)
        then(measurement.fields["phase_a_min_active_power"]).isEqualTo(4.0f)
        then(measurement.fields["phase_a_max_apparent_power"]).isEqualTo(5.0f)
        then(measurement.fields["phase_a_min_apparent_power"]).isEqualTo(6.0f)
        then(measurement.fields["phase_a_max_voltage"]).isEqualTo(7.0f)
        then(measurement.fields["phase_a_min_voltage"]).isEqualTo(8.0f)
        then(measurement.fields["phase_a_max_current"]).isEqualTo(9.0f)
        then(measurement.fields["phase_a_min_current"]).isEqualTo(10.0f)
        then(measurement.fields).doesNotContainKey("phase_a_fundamental_active_energy")
        then(measurement.fields).doesNotContainKey("phase_a_fundamental_active_returned_energy")
        then(measurement.fields).doesNotContainKey("phase_a_lagging_reactive_energy")
        then(measurement.fields).doesNotContainKey("phase_a_leading_reactive_energy")
    }

    @Test
    fun `should put fundamental, lagging and leading energy in live fields`() {
        val phaseA = validPhase().copy(
            fundamentalActiveEnergy = 11.0f,
            fundamentalActiveReturnedEnergy = 12.0f,
            laggingReactiveEnergy = 13.0f,
            leadingReactiveEnergy = 14.0f
        )
        val phaseB = validPhase().copy(
            fundamentalActiveEnergy = 21.0f,
            fundamentalActiveReturnedEnergy = 22.0f,
            laggingReactiveEnergy = 23.0f,
            leadingReactiveEnergy = 24.0f
        )
        val phaseC = validPhase().copy(
            fundamentalActiveEnergy = 31.0f,
            fundamentalActiveReturnedEnergy = 32.0f,
            laggingReactiveEnergy = 33.0f,
            leadingReactiveEnergy = 34.0f
        )

        val measurement = factory.create(validData(phaseA = phaseA, phaseB = phaseB, phaseC = phaseC), loggerId = 1, device = device, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.liveFields["phase_a_fundamental_active_energy"]).isEqualTo(11.0f)
        then(measurement.liveFields["phase_a_fundamental_active_returned_energy"]).isEqualTo(12.0f)
        then(measurement.liveFields["phase_a_lagging_reactive_energy"]).isEqualTo(13.0f)
        then(measurement.liveFields["phase_a_leading_reactive_energy"]).isEqualTo(14.0f)
        then(measurement.liveFields["phase_b_fundamental_active_energy"]).isEqualTo(21.0f)
        then(measurement.liveFields["phase_b_fundamental_active_returned_energy"]).isEqualTo(22.0f)
        then(measurement.liveFields["phase_b_lagging_reactive_energy"]).isEqualTo(23.0f)
        then(measurement.liveFields["phase_b_leading_reactive_energy"]).isEqualTo(24.0f)
        then(measurement.liveFields["phase_c_fundamental_active_energy"]).isEqualTo(31.0f)
        then(measurement.liveFields["phase_c_fundamental_active_returned_energy"]).isEqualTo(32.0f)
        then(measurement.liveFields["phase_c_lagging_reactive_energy"]).isEqualTo(33.0f)
        then(measurement.liveFields["phase_c_leading_reactive_energy"]).isEqualTo(34.0f)
        then(measurement.liveFields).hasSize(12)
    }

    @Test
    fun `should map all three phases`() {
        val phaseA = validPhase().copy(totalActiveEnergy = 1.0)
        val phaseB = validPhase().copy(totalActiveEnergy = 2.0)
        val phaseC = validPhase().copy(totalActiveEnergy = 3.0)

        val measurement = factory.create(validData(phaseA = phaseA, phaseB = phaseB, phaseC = phaseC), loggerId = 1, device = device, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["phase_a_total_active_energy"]).isEqualTo(1.0)
        then(measurement.fields["phase_b_total_active_energy"]).isEqualTo(2.0)
        then(measurement.fields["phase_c_total_active_energy"]).isEqualTo(3.0)
    }

    private fun validData(
        phaseA: EnergyPhase = validPhase(),
        phaseB: EnergyPhase = validPhase(),
        phaseC: EnergyPhase = validPhase()
    ) = EnergyMeasurement(
        mac = "mac",
        time = Instant.parse("2026-01-01T12:00:00Z"),
        phaseA = phaseA,
        phaseB = phaseB,
        phaseC = phaseC
    )

    private fun validPhase() = EnergyPhase(
        totalActiveEnergy = 100.0,
        totalActiveReturnedEnergy = 10.0,
        maxActivePower = 500.0f,
        minActivePower = 100.0f,
        maxApparentPower = 550.0f,
        minApparentPower = 110.0f,
        maxVoltage = 235.0f,
        minVoltage = 225.0f,
        maxCurrent = 5.0f,
        minCurrent = 1.0f,
        fundamentalActiveEnergy = 90.0f,
        fundamentalActiveReturnedEnergy = 8.0f,
        laggingReactiveEnergy = 5.0f,
        leadingReactiveEnergy = 3.0f
    )
}
