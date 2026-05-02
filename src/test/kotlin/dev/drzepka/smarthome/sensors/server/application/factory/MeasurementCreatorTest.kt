package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.ValidationResult
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Phase
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Pv
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.PvMeasurement
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureMeasurement
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
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
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.Measurement as MeasurementDto

@ExtendWith(MockitoExtension::class)
class MeasurementCreatorTest {

    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val activeDevice = Device(Group().apply { id = 10 }).apply { active = true }

    private val deviceRepository = mock<DeviceRepository> {
        on { findById(eq(1)) } doReturn activeDevice
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
        override fun create(input: TemperatureMeasurement, loggerId: Int, groupId: Int, time: Instant): Measurement {
            lastCreatedInput = input
            lastCreatedTime = time
            return Measurement(createdAt = time, deviceId = input.deviceId, loggerId = loggerId, groupId = groupId, type = "temperature", fields = emptyMap())
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
            override fun create(input: TemperatureMeasurement, loggerId: Int, groupId: Int, time: Instant) =
                throw UnsupportedOperationException()
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(), emptyList(), listOf(unsupportedFactory)
        )

        val throwable = catchThrowable { creator.create(validTemperatureInput(time = now), loggerId = 1, now = now) }

        then(throwable).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `should throw ValidationException for unknown device`() {
        val input = validTemperatureInput(deviceId = 9999, time = now)

        val throwable = catchThrowable { getCreator().create(input, loggerId = 1, now = now) }

        then(throwable).isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should throw ValidationException for measurement time in the future`() {
        val input = validTemperatureInput(time = now.plusSeconds(1))

        val throwable = catchThrowable { getCreator().create(input, loggerId = 1, now = now) }

        then(throwable).isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should throw ValidationException when validator returns invalid result`() {
        val rejectingValidator = object : MeasurementValidator<TemperatureMeasurement> {
            override fun supports(input: MeasurementDto) = input is TemperatureMeasurement
            override fun validate(input: TemperatureMeasurement) = ValidationResult.Invalid(emptyList())
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(),
            listOf(rejectingValidator), listOf(fakeTemperatureFactory)
        )

        val throwable = catchThrowable { creator.create(validTemperatureInput(time = now), loggerId = 1, now = now) }

        then(throwable).isInstanceOf(ValidationException::class.java)
    }

    @Test
    fun `should dispatch PV measurement to PV factory`() {
        val input = validPvInput(time = now)
        var pvCreated = false
        val fakePvFactory = object : MeasurementFactory<PvMeasurement> {
            override fun supports(input: MeasurementDto) = input is PvMeasurement
            override fun create(input: PvMeasurement, loggerId: Int, groupId: Int, time: Instant): Measurement {
                pvCreated = true
                return Measurement(createdAt = time, deviceId = input.deviceId, loggerId = loggerId, groupId = groupId, type = "pv", fields = emptyMap())
            }
        }
        val creator = MeasurementCreator(
            deviceRepository, MeasurementCommonValidator(), emptyList(), listOf(fakePvFactory)
        )

        creator.create(input, loggerId = 2, now = now)

        then(pvCreated).isTrue()
    }

    private fun validTemperatureInput(deviceId: Int = 1, time: Instant? = now) = TemperatureMeasurement(
        deviceId = deviceId,
        time = time,
        temperature = BigDecimal("21.0"),
        humidity = BigDecimal("55.0")
    )

    private fun validPvInput(deviceId: Int = 1, time: Instant? = now) = PvMeasurement(
        deviceId = deviceId,
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
