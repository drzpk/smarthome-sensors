package dev.drzepka.smarthome.sensors.server.application.dto.group

import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import java.time.Instant

class GroupResource {
    var id = 0
    var name = ""
    var description = ""
    var createdAt: Instant = Instant.now()

    companion object  {
        fun fromEntity(group: Group): GroupResource = GroupResource().apply {
            id = group.id!!
            name = group.name
            description = group.description
            createdAt = group.createdAt
        }
    }
}