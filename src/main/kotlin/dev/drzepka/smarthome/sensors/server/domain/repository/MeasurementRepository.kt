package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement

interface MeasurementRepository {
    suspend fun save(measurements: Collection<Measurement>)
}