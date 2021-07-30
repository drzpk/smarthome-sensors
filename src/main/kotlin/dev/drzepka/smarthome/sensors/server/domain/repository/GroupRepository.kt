package dev.drzepka.smarthome.sensors.server.domain.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Group

interface GroupRepository {
    fun findAll(): Collection<Group>
    fun findById(id: Int): Group?
    fun findByName(name: String): Group?
    fun save(group: Group)
    fun delete(groupId: Int)
}