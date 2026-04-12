package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ObjectError
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.CreateMeasurementsRequestV2
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureDataDTO
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import org.assertj.core.api.BDDAssertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class MeasurementFactoryTest {

    private val deviceRepository = mock<DeviceRepository> {
        on { findById(eq(1)) } doAnswer {
            val group = Group().apply { id = 1 }
            Device(group).apply {
                active = true
            }
        }
    }

    @Test
    fun `should build measurement`() {
        val now = Instant.now()
        val loggerId = 2
        val request = createValidMeasurementRequest()

        val measurement = getFactory().create(request, loggerId, now)

        then(measurement.createdAt).isEqualTo(now.minusMillis(200))
        then(measurement.deviceId).isEqualTo(1)
        then(measurement.loggerId).isEqualTo(2)
        then(measurement.groupId).isEqualTo(1)
        then(measurement.type).isEqualTo("temperature")
        then(measurement.fields["temperature"]).isEqualTo(BigDecimal("21.21"))
        then(measurement.fields["humidity"]).isEqualTo(BigDecimal("55.49"))
        then(measurement.fields["battery_voltage"]).isEqualTo(BigDecimal("3.192"))
        then(measurement.fields["battery_level"]).isEqualTo(84)
    }

    @Test
    fun `should build measurement without battery`() {
        val request = createValidMeasurementRequest()
        request.data = (request.data as TemperatureDataDTO).copy(batteryVoltage = null, batteryLevel = null)

        assertThatNoException()
            .isThrownBy { getFactory().create(request, 3, Instant.now()) }
    }

    @Test
    fun `should validate temperature`() {
        val request = createValidMeasurementRequest()
        request.data = (request.data as TemperatureDataDTO).copy(temperature = BigDecimal(-100))
        assertFieldError("temperature") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate humidity`() {
        val request = createValidMeasurementRequest()
        request.data = (request.data as TemperatureDataDTO).copy(humidity = BigDecimal(101))
        assertFieldError("humidity") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate battery voltage`() {
        val request = createValidMeasurementRequest()
        request.data = (request.data as TemperatureDataDTO).copy(batteryVoltage = BigDecimal("9.1"))
        assertFieldError("batteryVoltage") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate battery level`() {
        val request = createValidMeasurementRequest()
        request.data = (request.data as TemperatureDataDTO).copy(batteryLevel = -1)
        assertFieldError("batteryLevel") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate device`() {
        val request = createValidMeasurementRequest()
        request.deviceId = 9999
        assertFieldError("deviceId") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate negative time offset`() {
        val request = createValidMeasurementRequest()
        request.timestampOffsetMillis = -1
        assertFieldError("timestampOffsetMillis") {
            getFactory().create(request, 1)
        }
    }

    @Test
    fun `should validate old measurement`() {
        val request = createValidMeasurementRequest()
        request.timestampOffsetMillis = Duration.ofHours(25).toMillis()
        assertObjectError("Cannot create measurements older than") {
            getFactory().create(request, 1)
        }
    }

    private fun createValidMeasurementRequest(): CreateMeasurementsRequestV2.Measurement {
        return CreateMeasurementsRequestV2.Measurement().apply {
            deviceId = 1
            timestampOffsetMillis = 200
            data = TemperatureDataDTO(
                temperature = BigDecimal("21.211"),
                humidity = BigDecimal("55.489"),
                batteryVoltage = BigDecimal("3.1921"),
                batteryLevel = 84
            )
        }
    }

    private fun assertFieldError(fieldName: String, block: (() -> Unit)) {
        val throwable = catchThrowable { block.invoke() }
        then(throwable).isInstanceOf(ValidationException::class.java)

        val validationException = throwable as ValidationException

        val hasFieldError = validationException.validationErrors.errors
            .any { it is FieldError && it.field == fieldName }

        then(hasFieldError)
            .withFailMessage("Field error '$fieldName' wasn't found")
    }

    @Suppress("SameParameterValue")
    private fun assertObjectError(messageContains: String, block: (() -> Unit)) {
        val throwable = catchThrowable { block.invoke() }
        then(throwable).isInstanceOf(ValidationException::class.java)

        val validationException = throwable as ValidationException

        val hasObjectError = validationException.validationErrors.errors
            .any { it is ObjectError && it.message.contains(messageContains) }

        then(hasObjectError)
            .withFailMessage("Object error with message containing '$messageContains' wasn't found")
    }

    private fun getFactory(): MeasurementFactory = MeasurementFactory(deviceRepository)
}
