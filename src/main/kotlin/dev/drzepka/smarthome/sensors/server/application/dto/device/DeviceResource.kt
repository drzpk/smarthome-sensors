package dev.drzepka.smarthome.sensors.server.application.dto.device

import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import java.time.Instant

class DeviceResource {
    var id = 0
    var name = ""
    var description = ""
    var mac = ""
    var createdAt: Instant = Instant.now()

    companion object {
        fun fromEntity(device: Device): DeviceResource = DeviceResource().apply {
            id = device.id!!
            name = device.name
            description = device.description
            mac = device.mac
            createdAt = device.createdAt
        }
    }
}