package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsResponse
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementFactory
import dev.drzepka.smarthome.sensors.server.application.util.LifespanTracker
import dev.drzepka.smarthome.sensors.server.application.util.describeErrors
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import java.time.Duration
import java.util.*

class MeasurementService(
    configurationProviderService: ConfigurationProviderService,
    taskScheduler: TaskScheduler,
    private val measurementFactory: MeasurementFactory,
    private val measurementRepository: MeasurementRepository
) {
    private val log by Logger()
    private val queue = LinkedList<Measurement>()
    private val measurementTracker: LifespanTracker<MeasurementInfo>

    init {
        val minInterval = configurationProviderService.getInt("measurements.minimumCreationIntervalSeconds")
        log.info("Setting minimum measurement interval to {} seconds", minInterval)
        measurementTracker = LifespanTracker(Duration.ofSeconds(minInterval.toLong()))

        taskScheduler.schedule("measurementSend", Duration.ofMinutes(1L), this::storeMeasurements)
    }

    // todo: stats per time interval and per device (DeviceStatsService?)
    fun createMeasurements(
        request: CreateMeasurementsRequest,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): CreateMeasurementsResponse {
        val response = CreateMeasurementsResponse()

        request.measurements.forEach {
            when (addMeasurement(it, logger)) {
                true -> response.created++
                false -> response.duplicated++
                null -> response.errors++
            }
        }

        log.debug(
            "Processed {} measurements from logger {} (created: {}, duplicated: {}, errors: {})",
            response.total, logger.id, response.created, response.duplicated, response.errors
        )
        return response
    }

    // Synchronization is required to prevent from inconsistency with measurement tracker when
    // two loggers post new measurements simultaneously
    @Synchronized
    private fun addMeasurement(
        single: CreateMeasurementsRequest.Measurement,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): Boolean? {
        return try {
            doAddMeasurement(single, logger)
        } catch (e: ValidationException) {
            val errors = e.validationErrors
                .describeErrors()
                .joinToString("\n") { "    - $it" }
            log.error("Mesurement {} didn't pass validation. \n  Errors: \n{}", single, errors)
            null
        } catch (e: Exception) {
            log.error("Error while creating measurement {}", single, e)
            null
        }
    }

    private fun doAddMeasurement(
        single: CreateMeasurementsRequest.Measurement,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): Boolean {

        // The deviceId variable alone is sufficient to track duplicated measurements.
        val measurementInfo = MeasurementInfo(single.deviceId)

        val measurement = measurementFactory.create(single, logger.id!!)
        if (measurementTracker.exists(measurement.createdAt, measurementInfo)) {
            log.debug(
                "Measurement from device {} has been already created within the minimum interval",
                single.deviceId
            )
            return false
        }

        synchronized(queue) { queue.add(measurement) }
        log.trace("Added measurement {} to queue. New size: {}", measurement.createdAt, queue.size)

        measurementTracker.track(measurement.createdAt, measurementInfo)

        return true
    }

    private suspend fun storeMeasurements() {
        log.debug("Storing {} measurements", queue.size)
        val clone = synchronized(queue) {
            val ret = ArrayList(queue)
            queue.clear()
            ret
        }
        measurementRepository.save(clone)
    }

    private data class MeasurementInfo(val deviceId: Int)

}