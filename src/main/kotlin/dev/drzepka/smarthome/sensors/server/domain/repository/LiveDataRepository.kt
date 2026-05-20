package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement

interface LiveDataRepository {
    fun save(measurement: Measurement)
    fun findByDeviceId(deviceId: Int): Map<String, Number?>?
}
