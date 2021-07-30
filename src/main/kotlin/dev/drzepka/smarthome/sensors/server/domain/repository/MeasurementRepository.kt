package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement

interface MeasurementRepository {
    suspend fun save(groupId: Int, measurements: Collection<Measurement>)
}