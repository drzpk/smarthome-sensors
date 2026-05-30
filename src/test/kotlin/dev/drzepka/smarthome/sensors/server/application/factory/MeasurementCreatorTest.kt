package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Pv
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.PvMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.TemperatureMeasurement
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import org.assertj.core.api.BDDAssertions.catchThrowable
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

@ExtendWith(MockitoExtension::class)
class MeasurementCreatorTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val activeDevice = Device(Group().apply { id = 10 }).apply {
        id = 1
        active = true
    }

    private val deviceRepository = mock<DeviceRepository> {
        on { findByMac(eq("mac")) } doReturn activeDevice
    }

    private var lastValidatedInput: MeasurementDto? = null
    private var lastCreatedInput: MeasurementDto? = null
    private var lastCreatedTime: Instant? = null

    private val fakeTemperatureValidator = object : MeasurementValidator<TemperatureMeasurement> {
        override fun supports(input: MeasurementDto) = input is TemperatureMeasurement
        override fun validate(input: TemperatureMeasurement): ValidationResult {
            lastValidatedInput = input
            return ValidationResult.Valid
        }
    }

    private val fakeTemperatureFactory = object : MeasurementFactory<TemperatureMeasurement> {
        override fun supports(input: MeasurementDto) = input is TemperatureMeasurement
        override fun create(input: TemperatureMeasurement, loggerId: Int, device: Device, time: Instant): Measurement {
            lastCreatedInput = input
            lastCreatedTime = time
            return Measurement(createdAt = time, deviceId = device.id!!, loggerId = loggerId, groupId = device.group?.id!!, type = "temperature", fields = emptyMap())
        }
    }

    @Test
    fun `should dispatch to matching factory`() {
        val input = validTemperatureInput(time = now)

        getCreator().create(input, loggerId = 2, now = now)

        then(lastCreatedInput).isSameAs(input)
    }

    @Test
    fun `should invoke matching validator before factory`() {
        val input = validTemperatureInput(time = now)

        getCreator().create(input, loggerId = 2, now = now)

        then(lastValidatedInput).isSameAs(input)
    }

    @Test
    fun `should use current time when measurement time is null`() {
        val input = validTemperatureInput(time = null)

        getCreator().create(input, loggerId = 2, now = now)

        then(lastCreatedTime).isEqualTo(now)
    }

    @Test
    fun `should throw when no factory supports the input type`() {
        val unsupportedFactory = object : MeasurementFactory<TemperatureMeasurement> {
            override fun supports(input: MeasurementDto) = false
            override fun create(input: TemperatureMeasurement, loggerId: Int, device: Device, time: Instant) =
                throw UnsupportedOperationException()
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(), emptyList(), listOf(unsupportedFactory)
        )

        val throwable = catchThrowable { creator.create(validTemperatureInput(time = now), loggerId = 1, now = now) }

        then(throwable).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `should return UnknownDevice for unknown MAC`() {
        val input = validTemperatureInput(mac = "unknown-mac", time = now)

        val result = getCreator().create(input, loggerId = 1, now = now)

        then(result).isInstanceOf(MeasurementCreationResult.UnknownDevice::class.java)
        then((result as MeasurementCreationResult.UnknownDevice).mac).isEqualTo("unknown-mac")
    }

    @Test
    fun `should return ValidationFailed for measurement time in the future`() {
        val input = validTemperatureInput(time = now.plusSeconds(1))

        val result = getCreator().create(input, loggerId = 1, now = now)

        then(result).isInstanceOf(MeasurementCreationResult.ValidationFailed::class.java)
    }

    @Test
    fun `should return ValidationFailed when validator returns invalid result`() {
        val rejectingValidator = object : MeasurementValidator<TemperatureMeasurement> {
            override fun supports(input: MeasurementDto) = input is TemperatureMeasurement
            override fun validate(input: TemperatureMeasurement) = ValidationResult.Invalid(emptyList())
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(),
            listOf(rejectingValidator), listOf(fakeTemperatureFactory)
        )

        val result = creator.create(validTemperatureInput(time = now), loggerId = 1, now = now)

        then(result).isInstanceOf(MeasurementCreationResult.ValidationFailed::class.java)
    }

    @Test
    fun `should dispatch PV measurement to PV factory`() {
        val input = validPvInput(time = now)
        var pvCreated = false
        val fakePvFactory = object : MeasurementFactory<PvMeasurement> {
            override fun supports(input: MeasurementDto) = input is PvMeasurement
            override fun create(input: PvMeasurement, loggerId: Int, device: Device, time: Instant): Measurement {
                pvCreated = true
                return Measurement(createdAt = time, deviceId = device.id!!, loggerId = loggerId, groupId = device.group?.id!!, type = "pv", fields = emptyMap())
            }
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(), emptyList(), listOf(fakePvFactory)
        )

        creator.create(input, loggerId = 2, now = now)

        then(pvCreated).isTrue()
    }

    private fun validTemperatureInput(mac: String = "mac", time: Instant? = now) = TemperatureMeasurement(
        mac = mac,
        time = time,
        temperature = BigDecimal("21.0"),
        humidity = BigDecimal("55.0")
    )

    private fun validPvInput(mac: String = "mac", time: Instant? = now) = PvMeasurement(
        mac = mac,
        time = time,
        totalPower = 3000,
        energyToday = BigDecimal("12.5"),
        energyTotal = BigDecimal("1500.0"),
        phaseA = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseB = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        phaseC = Phase(voltage = 230.0f, current = 4.3f, power = 1000, frequency = 50.0f),
        pv1 = Pv(voltage = 350.0f, current = 4.3f, power = 1500, energyToday = BigDecimal("6.2")),
        pv2 = null
    )

    private fun getCreator() = MeasurementCreator(
        deviceRepository, MeasurementCommonValidator(),
        listOf(fakeTemperatureValidator), listOf(fakeTemperatureFactory)
    )
}
