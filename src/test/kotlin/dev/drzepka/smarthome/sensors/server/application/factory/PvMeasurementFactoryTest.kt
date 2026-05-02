package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Pv
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.PvMeasurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PvMeasurementFactoryTest {

    private val factory = PvMeasurementFactory()

    @Test
    fun `should build measurement with correct metadata`() {
        val time = Instant.parse("2026-01-01T12:00:00Z")
        val data = validData()

        val measurement = factory.create(data, loggerId = 2, groupId = 5, time = time)

        then(measurement.createdAt).isEqualTo(time)
        then(measurement.deviceId).isEqualTo(1)
        then(measurement.loggerId).isEqualTo(2)
        then(measurement.groupId).isEqualTo(5)
        then(measurement.type).isEqualTo(PvMeasurementFactory.TYPE)
    }

    @Test
    fun `should map all PV measurement fields`() {
        val pv1 = Pv(voltage = 350.0f, current = 4.3f, power = 1505, energyToday = BigDecimal("6.2"))
        val pv2 = Pv(voltage = 340.0f, current = 4.1f, power = 1394, energyToday = BigDecimal("5.8"))
        val phaseA = Phase(voltage = 231.0f, current = 4.4f, power = 1017, frequency = 50.0f)
        val phaseB = Phase(voltage = 229.0f, current = 4.2f, power = 961, frequency = 50.1f)
        val phaseC = Phase(voltage = 230.0f, current = 4.3f, power = 989, frequency = 49.9f)
        val data = validData(
            totalPower = 2967,
            energyToday = BigDecimal("12.0"),
            energyTotal = BigDecimal("1500.0"),
            phaseA = phaseA,
            phaseB = phaseB,
            phaseC = phaseC,
            pv1 = pv1,
            pv2 = pv2
        )

        val measurement = factory.create(data, loggerId = 1, groupId = 1, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["total_power"]).isEqualTo(2967)
        then(measurement.fields["energy_today"]).isEqualTo(BigDecimal("12.0"))
        then(measurement.fields["energy_total"]).isEqualTo(BigDecimal("1500.0"))
        then(measurement.fields["phase_a_voltage"]).isEqualTo(231.0f)
        then(measurement.fields["phase_a_current"]).isEqualTo(4.4f)
        then(measurement.fields["phase_a_power"]).isEqualTo(1017)
        then(measurement.fields["phase_a_frequency"]).isEqualTo(50.0f)
        then(measurement.fields["phase_b_voltage"]).isEqualTo(229.0f)
        then(measurement.fields["phase_b_current"]).isEqualTo(4.2f)
        then(measurement.fields["phase_b_power"]).isEqualTo(961)
        then(measurement.fields["phase_b_frequency"]).isEqualTo(50.1f)
        then(measurement.fields["phase_c_voltage"]).isEqualTo(230.0f)
        then(measurement.fields["phase_c_current"]).isEqualTo(4.3f)
        then(measurement.fields["phase_c_power"]).isEqualTo(989)
        then(measurement.fields["phase_c_frequency"]).isEqualTo(49.9f)
        then(measurement.fields["pv1_voltage"]).isEqualTo(350.0f)
        then(measurement.fields["pv1_current"]).isEqualTo(4.3f)
        then(measurement.fields["pv1_power"]).isEqualTo(1505)
        then(measurement.fields["pv1_energy_today"]).isEqualTo(BigDecimal("6.2"))
        then(measurement.fields["pv2_voltage"]).isEqualTo(340.0f)
        then(measurement.fields["pv2_current"]).isEqualTo(4.1f)
        then(measurement.fields["pv2_power"]).isEqualTo(1394)
        then(measurement.fields["pv2_energy_today"]).isEqualTo(BigDecimal("5.8"))
    }

    @Test
    fun `should map null values for absent PV panels`() {
        val data = validData(pv1 = null, pv2 = null)

        val measurement = factory.create(data, loggerId = 1, groupId = 1, time = Instant.parse("2026-01-01T12:00:00Z"))

        then(measurement.fields["pv1_voltage"]).isNull()
        then(measurement.fields["pv1_current"]).isNull()
        then(measurement.fields["pv1_power"]).isNull()
        then(measurement.fields["pv1_energy_today"]).isNull()
        then(measurement.fields["pv2_voltage"]).isNull()
        then(measurement.fields["pv2_current"]).isNull()
        then(measurement.fields["pv2_power"]).isNull()
        then(measurement.fields["pv2_energy_today"]).isNull()
    }

    private fun validData(
        totalPower: Int = 3000,
        energyToday: BigDecimal = BigDecimal("12.5"),
        energyTotal: BigDecimal = BigDecimal("1500.0"),
        phaseA: Phase = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseB: Phase = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseC: Phase = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        pv1: Pv? = Pv(voltage = 350.0f, current = 4.3f, power = 1500, energyToday = BigDecimal("6.2")),
        pv2: Pv? = Pv(voltage = 350.0f, current = 4.3f, power = 1500, energyToday = BigDecimal("6.3"))
    ) = PvMeasurement(
        deviceId = 1,
        time = Instant.parse("2026-01-01T12:00:00Z"),
        totalPower = totalPower,
        energyToday = energyToday,
        energyTotal = energyTotal,
        phaseA = phaseA,
        phaseB = phaseB,
        phaseC = phaseC,
        pv1 = pv1,
        pv2 = pv2
    )
}
