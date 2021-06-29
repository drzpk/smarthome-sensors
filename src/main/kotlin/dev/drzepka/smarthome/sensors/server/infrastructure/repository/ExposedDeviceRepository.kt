package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Devices
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

class ExposedDeviceRepository : DeviceRepository {

    override fun findById(id: Int): Device? {
        return Devices.select { Devices.id eq id }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun findByNameAndActive(name: String, active: Boolean): Device? {
        return Devices.select { (Devices.name eq name) and (Devices.active eq active) }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun findAll(active: Boolean?): Collection<Device> {
        val query = if (active != null)
            Devices.select { Devices.active eq active }
        else
            Devices.selectAll()

        return query.map { rowToEntity(it) }
    }

    override fun save(device: Device) {
        if (device.isStored()) {
            Devices.update({ Devices.id eq device.id }) {
                entityToRow(device, it)
            }
        } else {
            val id = Devices.insertAndGetId {
                entityToRow(device, it)
            }

            device.id = id.value
        }
    }

    private fun entityToRow(entity: Device, stmt: UpdateBuilder<Int>) {
        stmt[Devices.name] = entity.name
        stmt[Devices.description] = entity.description
        stmt[Devices.mac] = entity.mac
        stmt[Devices.createdAt] = entity.createdAt
        stmt[Devices.active] = entity.active
    }

    private fun rowToEntity(row: ResultRow): Device {
        return Device().apply {
            id = row[Devices.id].value
            name = row[Devices.name]
            description = row[Devices.description]
            mac = row[Devices.mac]
            createdAt = row[Devices.createdAt]
            active = row[Devices.active]
        }
    }
}