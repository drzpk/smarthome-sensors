package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import org.assertj.core.api.BDDAssertions.then
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant

class ExposedDeviceRepositoryIntTest : BaseIntegrationTest() {

    private val groupRepository = ExposedGroupRepository()
    private val repository = ExposedDeviceRepository(groupRepository)

    @Test
    fun `should return empty collection when no devices exist`() {
        val result = transaction { repository.findAll() }
        then(result).isEmpty()
    }

    @Test
    fun `should save and find device by id`() {
        val group = savedGroup()
        val device = transaction {
            repository.save(newDevice("Thermometer", group))
            repository.findByNameAndActive("Thermometer", true)!!
        }

        val found = transaction { repository.findById(device.id!!) }

        then(found).isNotNull
        then(found!!.name).isEqualTo("Thermometer")
        then(found.type).isEqualTo("temperature")
        then(found.active).isTrue
        then(found.group!!.id).isEqualTo(group.id)
    }

    @Test
    fun `should find device by name and active status`() {
        val group = savedGroup()
        transaction {
            repository.save(newDevice("active-device", group, active = true))
            repository.save(newDevice("inactive-device", group, active = false))
        }

        val active = transaction { repository.findByNameAndActive("active-device", true) }
        val notFound = transaction { repository.findByNameAndActive("active-device", false) }

        then(active).isNotNull
        then(notFound).isNull()
    }

    @Test
    fun `should count devices by group`() {
        val group1 = savedGroup("Group 1")
        val group2 = savedGroup("Group 2")
        transaction {
            repository.save(newDevice("Device A", group1))
            repository.save(newDevice("Device B", group1))
            repository.save(newDevice("Device C", group2))
        }

        val count1 = transaction { repository.countByGroupId(group1.id!!) }
        val count2 = transaction { repository.countByGroupId(group2.id!!) }

        then(count1).isEqualTo(2)
        then(count2).isEqualTo(1)
    }

    @Test
    fun `should update device`() {
        val group = savedGroup()
        val device = transaction {
            repository.save(newDevice("Old Name", group))
            repository.findByNameAndActive("Old Name", true)!!
        }

        transaction {
            device.name = "New Name"
            device.active = false
            repository.save(device)
        }

        val updated = transaction { repository.findById(device.id!!) }
        then(updated!!.name).isEqualTo("New Name")
        then(updated.active).isFalse
    }

    @Test
    fun `should return only active devices when filtered`() {
        val group = savedGroup()
        transaction {
            repository.save(newDevice("active", group, active = true))
            repository.save(newDevice("inactive", group, active = false))
        }

        val activeOnly = transaction { repository.findAll(active = true) }

        then(activeOnly).hasSize(1)
        then(activeOnly.first().name).isEqualTo("active")
    }

    private fun savedGroup(name: String = "Test Group"): Group {
        return transaction {
            val group = Group().apply {
                this.name = name
                description = "desc"
                createdAt = Instant.now()
            }
            groupRepository.save(group)
            group
        }
    }

    private fun newDevice(name: String, group: Group, active: Boolean = true) = Device(group).apply {
        this.name = name
        description = "A sensor"
        type = "temperature"
        mac = "AA:BB:CC:DD:EE:FF"
        createdAt = Instant.now()
        this.active = active
    }
}
