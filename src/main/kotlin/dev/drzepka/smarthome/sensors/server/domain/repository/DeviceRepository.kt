package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Device

interface DeviceRepository {
    fun findById(id: Int): Device?
    fun findByNameAndActive(name: String, active: Boolean): Device?
    fun findAll(active: Boolean? = null): Collection<Device>
    fun save(device: Device)
}