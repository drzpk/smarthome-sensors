package dev.drzepka.smarthome.sensors.server.domain.service

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementFactory
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import dev.drzepka.smarthome.sensors.server.application.service.MeasurementService
import dev.drzepka.smarthome.sensors.server.application.service.TaskScheduler
import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
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
    private val measurementFactory = mock<MeasurementFactory>()
    private val measurementRepository = mock<MeasurementRepository>()

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
        whenever(measurementFactory.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(2))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(2)
        then(status.created).isEqualTo(2)
        verify(measurementFactory, times(2)).create(any(), any(), any())

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved(Pair(0, listOf(measurement1, measurement2)))
    }

    @Test
    fun `should create measurements - duplicates`() = runBlocking {
        val measurement1 = getMeasurement(0)
        val measurement2 = getMeasurement(0)
        whenever(measurementFactory.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(2)
        then(status.created).isEqualTo(1)
        then(status.duplicated).isEqualTo(1)

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved(Pair(0, listOf(measurement1)))
    }

    @Test
    fun `should create measurements - validation errors`() = runBlocking {
        whenever(measurementFactory.create(any(), any(), any())).thenThrow(ValidationException(ValidationErrors()))

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(1)
        then(status.errors).isEqualTo(1)

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved()
    }

    @Test
    fun `should create measurements - other errors`() = runBlocking {
        whenever(
            measurementFactory.create(any(), any(), any())
        ).thenThrow(IllegalStateException("something bad happened"))

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))

        val status = getService().createMeasurements(request, getLogger())

        then(status.total).isEqualTo(1)
        then(status.errors).isEqualTo(1)

        taskSchedulerAction!!.invoke()
        verifyMeasurementsSaved()
    }

    @Test
    fun `should store measurements`() = runBlocking {
        val measurement = getMeasurement(0)
        whenever(measurementFactory.create(any(), any(), any())).thenReturn(measurement)

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

        whenever(measurementFactory.create(any(), any(), any())).thenReturn(measurement1, measurement2)

        val request = CreateMeasurementsRequest()
        request.measurements.add(getRequestMeasurement(1))
        request.measurements.add(getRequestMeasurement(2))

        val service = getService()

        service.createMeasurements(request, getLogger())
        verify(measurementRepository, times(0)).save(any(), any())

        taskSchedulerAction!!.invoke()

        verifyMeasurementsSaved(Pair(0, listOf(measurement1)), Pair(1, listOf(measurement2)))
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

    private fun getMeasurement(groupId: Int): Measurement = Measurement(Instant.now(), 1, 2, groupId)

    private fun getRequestMeasurement(deviceId: Int): CreateMeasurementsRequest.Measurement {
        return CreateMeasurementsRequest.Measurement().apply {
            this.deviceId = deviceId
        }
    }

    private fun getLogger(): Logger = Logger().apply {
        id = 1
    }

    private fun getService(): MeasurementService = MeasurementService(
        configurationProviderService,
        taskScheduler,
        measurementFactory,
        measurementRepository
    )
}