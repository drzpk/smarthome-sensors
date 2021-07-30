package dev.drzepka.smarthome.sensors.server.infrastructure.repository.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object Groups : IntIdTable("groups") {
    val name = varchar("name", 64)
    val description = varchar("description", 256)
    val createdAt = timestamp("created_at")
}