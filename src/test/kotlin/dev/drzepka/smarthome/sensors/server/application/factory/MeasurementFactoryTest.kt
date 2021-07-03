package dev.drzepka.smarthome.sensors.server.application.factory

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ObjectError
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import org.assertj.core.api.BDDAssertions.catchThrowable
import org.assertj.core.api.BDDAssertions.then
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
            Device().apply {
                active = true
            }
        }
    }

    @Test
    fun `should build measurement`() {
        val now = Instant.now()
        val loggerId = 2
        val request = createValidMeasurementRequest()

        val measurement = getBuilder().create(request, loggerId, now)

        then(measurement.createdAt).isEqualTo(now.minusMillis(200))
        then(measurement.deviceId).isEqualTo(1)
        then(measurement.loggerId).isEqualTo(2)
        then(measurement.temperature).isEqualTo(BigDecimal("21.21"))
        then(measurement.humidity).isEqualTo(BigDecimal("55.49"))
        then(measurement.batteryVoltage).isEqualTo(BigDecimal("3.192"))
        then(measurement.batteryLevel).isEqualTo(84)
    }

    @Test
    fun `should validate temperature`() {
        val request = createValidMeasurementRequest()
        request.temperature = BigDecimal(-100)
        assertFieldError("temperature") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate humidity`() {
        val request = createValidMeasurementRequest()
        request.humidity = BigDecimal(101)
        assertFieldError("humidity") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate battery voltage`() {
        val request = createValidMeasurementRequest()
        request.batteryVoltage = BigDecimal("9.1")
        assertFieldError("batteryVoltage") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate battery level`() {
        val request = createValidMeasurementRequest()
        request.batteryLevel = -1
        assertFieldError("batteryLevel") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate device`() {
        val request = createValidMeasurementRequest()
        request.deviceId = 9999
        assertFieldError("deviceId") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate negative time offset`() {
        val request = createValidMeasurementRequest()
        request.timestampOffsetMillis = -1
        assertFieldError("timestampOffsetMillis") {
            getBuilder().create(request, 1)
        }
    }

    @Test
    fun `should validate old measurement`() {
        val request = createValidMeasurementRequest()
        request.timestampOffsetMillis = Duration.ofHours(25).toMillis()
        assertObjectError("Cannot create measurements older than") {
            getBuilder().create(request, 1)
        }
    }

    private fun createValidMeasurementRequest(): CreateMeasurementsRequest.Measurement {
        return CreateMeasurementsRequest.Measurement().apply {
            deviceId = 1
            temperature = BigDecimal("21.211")
            humidity = BigDecimal("55.489")
            batteryVoltage = BigDecimal("3.1921")
            batteryLevel = 84
            timestampOffsetMillis = 200
        }
    }

    private fun assertFieldError(fieldName: String, block: (() -> Unit)) {
        val throwable = catchThrowable { block.invoke() }
        then(throwable).isInstanceOf(ValidationException::class.java)

        val validationException = throwable as ValidationException

        val hasFieldErrror = validationException.validationErrors.errors
            .any { it is FieldError && it.field == fieldName }

        then(hasFieldErrror)
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

    private fun getBuilder(): MeasurementFactory = MeasurementFactory(deviceRepository)
}