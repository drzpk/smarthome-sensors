package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsResponse
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementCreator
import dev.drzepka.smarthome.sensors.server.application.util.LifespanTracker
import dev.drzepka.smarthome.sensors.server.application.util.describeErrors
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.LiveDataRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class MeasurementService(
    configurationProviderService: ConfigurationProviderService,
    taskScheduler: TaskScheduler,
    private val measurementCreator: MeasurementCreator,
    private val measurementRepository: MeasurementRepository,
    private val liveDataRepository: LiveDataRepository
) {
    private val log by Logger()
    private val queue = ArrayDeque<Measurement>()
    private val queueMutex = Mutex()
    private val minInterval: Duration
    private val measurementTracker: LifespanTracker<Int>

    init {
        val minIntervalSeconds = configurationProviderService.getInt("measurements.minimumCreationIntervalSeconds")
        minInterval = Duration.ofSeconds(minIntervalSeconds.toLong())
        log.info("Setting minimum measurement interval to {} seconds", minIntervalSeconds)
        measurementTracker = LifespanTracker(minInterval)

        taskScheduler.schedule("measurementStorage", Duration.ofSeconds(30L), this::storeMeasurements)
    }

    // todo: stats per time interval and per device (DeviceStatsService?)
    suspend fun createMeasurements(
        request: CreateMeasurementsRequest,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): CreateMeasurementsResponse {
        val response = CreateMeasurementsResponse()

        request.measurements
            .sortedWith(compareBy(nullsLast()) { it.time })
            .forEach {
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

    private suspend fun addMeasurement(
        single: MeasurementDto,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): Boolean? {
        return try {
            doAddMeasurement(single, logger)
        } catch (e: ValidationException) {
            val errors = e.validationErrors
                .describeErrors()
                .joinToString("\n") { "    - $it" }
            log.error("Measurement {} didn't pass validation. \n  Errors: \n{}", single, errors)
            null
        } catch (e: Exception) {
            log.error("Error while creating measurement {}", single, e)
            null
        }
    }

    private suspend fun doAddMeasurement(
        single: MeasurementDto,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): Boolean {
        val deviceId = single.deviceId
        val measurement = measurementCreator.create(single, logger.id!!)
        liveDataRepository.save(measurement)

        if (!measurementTracker.isTracked(deviceId))
            initializeTrackerForDevice(measurement.groupId, deviceId, measurement.createdAt)

        if (measurementTracker.existsOrTrack(measurement.createdAt, deviceId)) {
            log.debug(
                "Measurement from device {} has been already created within the minimum interval",
                deviceId
            )
            return false
        }

        queueMutex.withLock { queue.add(measurement) }

        return true
    }

    private suspend fun initializeTrackerForDevice(groupId: Int, deviceId: Int, referenceTime: Instant) {
        val since = referenceTime.minus(minInterval)
        val latestTime = measurementRepository.findLatestMeasurementTime(groupId, deviceId, since)

        // Always track to mark device as seen and prevent repeated DB queries
        measurementTracker.track(latestTime ?: Instant.EPOCH, deviceId)
        if (latestTime != null)
            log.debug("Initialized tracker for device {} with latest DB measurement at {}", deviceId, latestTime)
    }

    private suspend fun storeMeasurements() {
        log.debug("Storing {} measurements", queue.size)
        val clone = queueMutex.withLock {
            val ret = ArrayList(queue)
            queue.clear()
            ret
        }

        clone.groupBy { it.groupId }
            .forEach { group ->
                try {
                    measurementRepository.save(group.key, group.value)
                } catch (_: Exception) {
                    log.error("Error while storing {} measurements for group {}", group.value.size, group.key)
                }
            }
    }
}
