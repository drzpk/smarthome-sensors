package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Devices
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.util.countRows
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

class ExposedDeviceRepository(private val groupRepository: GroupRepository) : DeviceRepository {

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

    override fun countByGroupId(groupId: Int): Int {
        return Devices.countRows(Op.build { Devices.groupId eq groupId }).toInt()
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
        stmt[Devices.groupId] = entity.group?.id
    }

    @Suppress("UNNECESSARY_SAFE_CALL", "RedundantNullableReturnType")
    private fun rowToEntity(row: ResultRow): Device {
        val groupId: Int? = row[Devices.groupId]?.value
        val group = if (groupId != null) groupRepository.findById(groupId) else null

        return Device(group).apply {
            id = row[Devices.id].value
            name = row[Devices.name]
            description = row[Devices.description]
            mac = row[Devices.mac]
            createdAt = row[Devices.createdAt]
            active = row[Devices.active]
        }
    }
}