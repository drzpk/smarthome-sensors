package dev.drzepka.smarthome.sensors.server.infrastructure.repository.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object Devices : IntIdTable("devices") {
    val name = varchar("name", 64)
    val description = varchar("description", 256)
    val mac = varchar("mac", 64)
    val createdAt = timestamp("created_at")
    var active = bool("active")
}