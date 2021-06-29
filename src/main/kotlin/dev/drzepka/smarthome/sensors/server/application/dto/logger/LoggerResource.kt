package dev.drzepka.smarthome.sensors.server.application.dto.logger

import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import java.time.Instant

class LoggerResource {
    var id = 0
    var name = ""
    var description = ""
    var createdAt: Instant = Instant.now()
    var password: String? = null

    companion object {
        fun fromEntity(entity: Logger, password: String? = null): LoggerResource {
            return LoggerResource().apply {
                id = entity.id!!
                name = entity.name
                description = entity.description
                createdAt = entity.createdAt

                password?.let { this.password = it }
            }
        }
    }
}