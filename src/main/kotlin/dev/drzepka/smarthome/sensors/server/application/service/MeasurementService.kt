package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsResponse
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementCreationResult
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementCreator
import dev.drzepka.smarthome.sensors.server.application.util.LifespanTracker
import dev.drzepka.smarthome.sensors.server.application.util.describeErrors
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.LiveDataRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.Measurement as MeasurementDto

class MeasurementService(
    configurationProvider: ConfigurationProviderService,
    taskScheduler: TaskScheduler,
    private val measurementCreator: MeasurementCreator,
    private val measurementRepository: MeasurementRepository,
    private val liveDataRepository: LiveDataRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    private val log by Logger()
    private val queue = ArrayDeque<Measurement>()
    private val queueMutex = Mutex()
    private val minInterval: Duration
    private val measurementTracker: LifespanTracker<Int>

    init {
        val minIntervalSeconds = configurationProvider.config.getInt("measurements.minimumCreationIntervalSeconds")
        minInterval = Duration.ofSeconds(minIntervalSeconds.toLong())
        log.info("Setting minimum measurement interval to {} seconds", minIntervalSeconds)
        measurementTracker = LifespanTracker(minInterval)

        taskScheduler.schedule("measurementStorage", Duration.ofSeconds(30L), this::storeMeasurements)
    }

    suspend fun createMeasurements(
        request: CreateMeasurementsRequest,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): CreateMeasurementsResponse {
        var created = 0
        var duplicated = 0
        var errors = 0
        val unknown = linkedSetOf<String>()

        request.measurements
            .sortedWith(compareBy(nullsLast()) { it.time })
            .forEach {
                when (val result = addMeasurement(it, logger)) {
                    AddResult.Created -> created++
                    AddResult.Duplicated -> duplicated++
                    AddResult.Error -> errors++
                    is AddResult.UnknownDevice -> {
                        if (unknown.add(result.mac))
                            log.warn("Device with MAC {} is not registered", result.mac)
                    }
                }
            }

        log.debug(
            "Processed {} measurements from logger {} (created: {}, duplicated: {}, errors: {}, unknown: {})",
            created + duplicated + errors + unknown.size, logger.id, created, duplicated, errors, unknown.size
        )
        return CreateMeasurementsResponse(created, duplicated, errors, unknown.toList())
    }

    private suspend fun addMeasurement(
        single: MeasurementDto,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): AddResult {
        return try {
            doAddMeasurement(single, logger)
        } catch (e: Exception) {
            log.error("Error while creating measurement {}", single, e)
            AddResult.Error
        }
    }

    private suspend fun doAddMeasurement(
        single: MeasurementDto,
        logger: dev.drzepka.smarthome.sensors.server.domain.entity.Logger
    ): AddResult {
        return when (val result = measurementCreator.create(single, logger.id!!, clock.instant())) {
            is MeasurementCreationResult.UnknownDevice -> AddResult.UnknownDevice(result.mac)
            is MeasurementCreationResult.ValidationFailed -> {
                val errorText = result.errors.describeErrors().joinToString("\n") { "    - $it" }
                log.error("Measurement {} didn't pass validation. \n  Errors: \n{}", single, errorText)
                AddResult.Error
            }
            is MeasurementCreationResult.Success -> {
                val measurement = result.measurement
                val deviceId = measurement.deviceId
                liveDataRepository.save(measurement)

                if (!measurementTracker.isTracked(deviceId))
                    initializeTrackerForDevice(measurement.groupId, deviceId, measurement.createdAt)

                if (measurementTracker.existsOrTrack(measurement.createdAt, deviceId)) {
                    log.debug(
                        "Measurement from device {} has been already created within the minimum interval",
                        deviceId
                    )
                    return AddResult.Duplicated
                }

                queueMutex.withLock { queue.add(measurement) }
                AddResult.Created
            }
        }
    }

    private suspend fun initializeTrackerForDevice(groupId: Int, deviceId: Int, referenceTime: Instant) {
        val since = referenceTime.minus(minInterval)
        val latestTime = measurementRepository.findLatestMeasurementTime(groupId, deviceId, since)

        // Always track to mark device as seen and prevent repeated DB queries
        measurementTracker.track(latestTime ?: Instant.EPOCH, deviceId)
        if (latestTime != null)
            log.debug("Initialized tracker for device {} with latest DB measurement at {}", deviceId, latestTime)
    }

    internal suspend fun storeMeasurements() {
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

    private sealed interface AddResult {
        object Created : AddResult
        object Duplicated : AddResult
        object Error : AddResult
        data class UnknownDevice(val mac: String) : AddResult
    }
}
