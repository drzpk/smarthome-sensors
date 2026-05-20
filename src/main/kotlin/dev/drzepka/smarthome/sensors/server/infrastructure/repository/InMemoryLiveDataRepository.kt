package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.LiveDataRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryLiveDataRepository : LiveDataRepository {

    private val data = ConcurrentHashMap<Int, Entry>()

    override fun save(measurement: Measurement) {
        if (measurement.liveFields.isEmpty()) return
        data.compute(measurement.deviceId) { _, existing ->
            if (existing == null || measurement.createdAt.isAfter(existing.createdAt))
                Entry(measurement.createdAt, measurement.liveFields)
            else
                existing
        }
    }

    override fun findByDeviceId(deviceId: Int): Map<String, Number?>? = data[deviceId]?.fields

    private data class Entry(val createdAt: Instant, val fields: Map<String, Number?>)
}
