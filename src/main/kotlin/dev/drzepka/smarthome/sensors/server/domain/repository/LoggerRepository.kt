package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Logger

interface LoggerRepository {
    fun findById(id: Int): Logger?
    fun findByNameAndActive(name: String, active: Boolean): Logger?
    fun findAll(active: Boolean? = null): Collection<Logger>
    fun save(logger: Logger)
}