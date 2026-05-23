package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import java.time.Instant

interface MeasurementRepository {
    suspend fun save(groupId: Int, measurements: Collection<Measurement>)
    suspend fun findLatestMeasurementTime(groupId: Int, deviceId: Int, since: Instant): Instant?
}
