package dev.drzepka.smarthome.sensors.server.domain.service

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.TemperatureMeasurement
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementCreator
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import dev.drzepka.smarthome.sensors.server.application.service.MeasurementService
import dev.drzepka.smarthome.sensors.server.application.service.TaskScheduler
import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.LiveDataRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class MeasurementServiceTest {

    private val configurationProviderService = mock<ConfigurationProviderService> {
        on { getInt(eq("measurements.minimumCreationIntervalSeconds"), eq(null)) } doReturn 30
    }
    private val taskScheduler = mock<TaskScheduler> {
        on { schedule(any(), any(), any()) } doAnswer {
            taskSchedulerAction = it.getArgument(2) as (suspend () -> Unit)
        }
    }
    private val measurementCreator = mock<MeasurementCreator>()
    private val measurementRepository = mock<MeasurementRepository> {
        onBlocking { findLatestMeasurementTime(any(), any(), any()) } doReturn null
    }
    private val liveDataRepository = mock<LiveDataRepository>()

    private var taskSchedulerAction: (suspend () -> Unit)? = null

    @BeforeEach
    fun beforeEach() {
        taskSchedulerAction = null
    }

    @Test
    fun `should schedule storing measurements`() {
        getService()
        verify(taskScheduler).schedule(any(), any(), any())
    }

    @Test
    fun `should create measurements - positive case`() = runBlocking {
        val measurement1 = getMeasurement(0)
        val measurement2 = getMeasurement(0)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(2))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(2)
        then(status.created).isEqualTo(2)
        verify(measurementCreator, times(2)).create(any(), any(), any())

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved(Pair(0, listOf(measurement1, measurement2)))
    }

    @Test
    fun `should save live data when measurement is accepted`() = runBlocking {
        val measurement = getMeasurement(0)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        getService().createMeasurements(request, getLogger())

        verify(liveDataRepository).save(measurement)
    }

    @Test
    fun `should create measurements - duplicates`() = runBlocking {
        val measurement1 = getMeasurement(0)
        val measurement2 = getMeasurement(0)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(2)
        then(status.created).isEqualTo(1)
        then(status.duplicated).isEqualTo(1)
        verify(liveDataRepository, times(2)).save(any())

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved(Pair(0, listOf(measurement1)))
    }

    @Test
    fun `should save to live data repository even when measurement is rate limited`() = runBlocking {
        val measurement1 = getMeasurement(0)
        val measurement2 = getMeasurement(0)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(1))

        getService().createMeasurements(request, getLogger())

        verify(liveDataRepository).save(measurement1)
        verify(liveDataRepository).save(measurement2)
    }

    @Test
    fun `should create measurements - validation errors`() = runBlocking {
        whenever(measurementCreator.create(any(), any(), any())).thenThrow(ValidationException(ValidationErrors()))

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(1)
        then(status.errors).isEqualTo(1)
        verify(liveDataRepository, never()).save(any())

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved()
    }

    @Test
    fun `should create measurements - other errors`() = runBlocking {
        whenever(
            measurementCreator.create(any(), any(), any())
        ).thenThrow(IllegalStateException("something bad happened"))

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(1)
        then(status.errors).isEqualTo(1)
        verify(liveDataRepository, never()).save(any())

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved()
    }

    @Test
    fun `should store measurements`() = runBlocking {
        val measurement = getMeasurement(0)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val service = getService()

        service.createMeasurements(request, getLogger())
        verify(measurementRepository, times(0)).save(any(), any())

        taskSchedulerAction!!.invoke()

        verifyMeasurementsSaved(Pair(0, listOf(measurement)))

        reset(measurementRepository)
        taskSchedulerAction!!.invoke()

        verifyMeasurementsSaved()
    }

    @Test
    fun `should store measurements by groups`() = runBlocking {
        val measurement1 = getMeasurement(0)
        val measurement2 = getMeasurement(1)

        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(2))

        val service = getService()

        service.createMeasurements(request, getLogger())
        verify(measurementRepository, times(0)).save(any(), any())

        taskSchedulerAction!!.invoke()

        verifyMeasurementsSaved(Pair(0, listOf(measurement1)), Pair(1, listOf(measurement2)))
    }

    @Test
    fun `should initialize tracker from database for first measurement from a device`() = runBlocking {
        val dbTime = Instant.parse("2026-01-01T11:59:45Z")
        val measurementTime = Instant.parse("2026-01-01T12:00:00Z")
        wheneverBlocking { measurementRepository.findLatestMeasurementTime(eq(0), eq(1), any()) } doReturn dbTime

        val measurement = getMeasurement(0, measurementTime)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1, time = measurementTime))

        val status = getService().createMeasurements(request, getLogger())

        then(status.duplicated).isEqualTo(1)
        then(status.created).isEqualTo(0)
        verifyBlocking(measurementRepository) { findLatestMeasurementTime(eq(0), eq(1), any()) }
    }

    @Test
    fun `should accept measurement when no recent database measurement exists for device`(): Unit = runBlocking {
        val measurement = getMeasurement(0, Instant.parse("2026-01-01T12:00:00Z"))
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(measurement)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.created).isEqualTo(1)
        then(status.duplicated).isEqualTo(0)
    }

    @Test
    fun `should initialize tracker from database only once per device`() = runBlocking {
        val m1 = getMeasurement(0, Instant.parse("2026-01-01T12:00:00Z"))
        val m2 = getMeasurement(0, Instant.parse("2026-01-01T12:01:00Z"))
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(m1, m2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1, time = Instant.parse("2026-01-01T12:00:00Z")))
        request.measurements.add(getRequestMeasurement(1, time = Instant.parse("2026-01-01T12:01:00Z")))

        getService().createMeasurements(request, getLogger())

        verifyBlocking(measurementRepository, times(1)) { findLatestMeasurementTime(eq(0), eq(1), any()) }
    }

    @Test
    fun `should process measurements sorted by time`(): Unit = runBlocking {
        val t1 = Instant.parse("2026-01-01T12:00:00Z")
        val t2 = Instant.parse("2026-01-01T12:00:05Z")
        val t3 = Instant.parse("2026-01-01T12:00:15Z")

        val m1 = getMeasurement(0, t1)
        val m2 = getMeasurement(0, t2)
        val m3 = getMeasurement(0, t3)
        whenever(measurementCreator.create(any(), any(), any())).thenReturn(m1, m2, m3)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1, time = t3))
        request.measurements.add(getRequestMeasurement(1, time = t1))
        request.measurements.add(getRequestMeasurement(1, time = t2))

        val status = getService().createMeasurements(request, getLogger())

        then(status.created).isEqualTo(1)
        then(status.duplicated).isEqualTo(2)
    }

    private suspend fun verifyMeasurementsSaved(vararg measurements: Pair<Int, Collection<Measurement>>) {
        if (measurements.isNotEmpty()) {
            measurements.forEach {
                verify(measurementRepository).save(eq(it.first), eq(it.second))
            }
        }
        else {
            verify(measurementRepository, times(0)).save(any(), any())
        }
    }

    private fun getMeasurement(groupId: Int, createdAt: Instant = Instant.now()): Measurement =
        Measurement(createdAt, 1, 2, groupId, "temperature", emptyMap())

    private fun getRequestMeasurement(deviceId: Int, time: Instant? = Instant.parse("2026-01-01T12:00:00Z")): TemperatureMeasurement = TemperatureMeasurement(
        deviceId = deviceId,
        time = time,
        temperature = BigDecimal("21.0"),
        humidity = BigDecimal("55.0")
    )

    private fun getLogger(): Logger = Logger().apply {
        id = 1
    }

    private fun getService(): MeasurementService = MeasurementService(
        configurationProviderService,
        taskScheduler,
        measurementCreator,
        measurementRepository,
        liveDataRepository
    )
}
