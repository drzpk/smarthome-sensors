package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Groups
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder

class ExposedGroupRepository : GroupRepository {

    override fun findAll(): Collection<Group> {
        return Groups.selectAll()
            .map { rowToEntity(it) }
    }

    override fun findById(id: Int): Group? {
        return Groups.select { Groups.id eq id }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun findByName(name: String): Group? {
        return Groups.select { Groups.name eq name }
            .firstOrNull()
            ?.let { rowToEntity(it) }
    }

    override fun save(group: Group) {
        if (group.isStored()) {
            Groups.update({ Groups.id eq group.id }) {
                entityToRow(group, it)
            }
        } else {
            val id = Groups.insertAndGetId {
                entityToRow(group, it)
            }

            group.id = id.value
        }
    }

    override fun delete(groupId: Int) {
        Groups.deleteWhere { Groups.id eq groupId }
    }

    private fun entityToRow(entity: Group, stmt: UpdateBuilder<Int>) {
        stmt[Groups.name] = entity.name
        stmt[Groups.description] = entity.description
        stmt[Groups.createdAt] = entity.createdAt
    }

    private fun rowToEntity(row: ResultRow): Group {
        return Group().apply {
            id = row[Groups.id].value
            name = row[Groups.name]
            description = row[Groups.description]
            createdAt = row[Groups.createdAt]
        }
    }
}