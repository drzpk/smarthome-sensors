package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import dev.drzepka.smarthome.sensors.server.domain.repository.LoggerRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Loggers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

class ExposedLoggerRepository : LoggerRepository {

    override fun findById(id: Int): Logger? {
        return Loggers.select { Loggers.id eq id }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun findByNameAndActive(name: String, active: Boolean): Logger? {
        return Loggers.select { (Loggers.name eq name) and (Loggers.active eq active) }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun findAll(active: Boolean?): Collection<Logger> {
        val query = if (active != null)
            Loggers.select { Loggers.active eq active }
        else
            Loggers.selectAll()

        return query.map { rowToEntity(it) }
    }

    override fun save(logger: Logger) {
        if (logger.isStored()) {
            Loggers.update({ Loggers.id eq logger.id }) {
                entityToRow(logger, it)
            }
        } else {
            val id = Loggers.insertAndGetId {
                entityToRow(logger, it)
            }

            logger.id = id.value
        }
    }

    private fun entityToRow(entity: Logger, stmt: UpdateBuilder<Int>) {
        stmt[Loggers.name] = entity.name
        stmt[Loggers.description] = entity.description
        stmt[Loggers.password] = entity.password
        stmt[Loggers.createdAt] = entity.createdAt
        stmt[Loggers.active] = entity.active
    }

    private fun rowToEntity(row: ResultRow): Logger {
        return Logger().apply {
            id = row[Loggers.id].value
            name = row[Loggers.name]
            description = row[Loggers.description]
            password = row[Loggers.password]
            createdAt = row[Loggers.createdAt]
            active = row[Loggers.active]
        }
    }
}