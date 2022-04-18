package dev.drzepka.smarthome.sensors.server.application.dto.device

import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import java.time.Instant

class DeviceResource {
    var id = 0
    var name = ""
    var description = ""
    var type = ""
    var mac = ""
    var createdAt: Instant = Instant.now()
    var group: GroupResource? = null

    companion object {
        fun fromEntity(device: Device): DeviceResource = DeviceResource().apply {
            id = device.id!!
            name = device.name
            description = device.description
            type = device.type
            mac = device.mac
            createdAt = device.createdAt
            group = device.group?.let { GroupResource.fromEntity(it) }
        }
    }
}