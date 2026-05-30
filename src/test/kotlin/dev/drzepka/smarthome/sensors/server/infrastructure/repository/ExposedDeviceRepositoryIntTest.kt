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
    fun `should return all devices regardless of active status`() {
        val group = savedGroup()
        transaction {
            repository.save(device("active-one", group, active = true))
            repository.save(device("inactive-one", group, active = false))
            repository.save(device("active-two", group, active = true))
        }

        val result = transaction { repository.findAll() }

        then(result).hasSize(3)
        then(result.map { it.name }).containsExactlyInAnyOrder("active-one", "inactive-one", "active-two")
    }

    @Test
    fun `should return only active devices when filtered by active`() {
        val group = savedGroup()
        transaction {
            repository.save(device("active", group, active = true))
            repository.save(device("inactive", group, active = false))
        }

        val result = transaction { repository.findAll(active = true) }

        then(result).hasSize(1)
        then(result.first().name).isEqualTo("active")
    }

    @Test
    fun `should return only inactive devices when filtered by inactive`() {
        val group = savedGroup()
        transaction {
            repository.save(device("active", group, active = true))
            repository.save(device("inactive", group, active = false))
        }

        val result = transaction { repository.findAll(active = false) }

        then(result).hasSize(1)
        then(result.first().name).isEqualTo("inactive")
    }

    @Test
    fun `should return device with all fields when found by id`() {
        val group = savedGroup()
        val saved = transaction {
            repository.save(device("thermometer", group))
            repository.findByNameAndActive("thermometer", true)!!
        }

        val result = transaction { repository.findById(saved.id!!) }

        then(result).isNotNull
        then(result!!.id).isEqualTo(saved.id)
        then(result.name).isEqualTo("thermometer")
        then(result.description).isEqualTo("Test sensor")
        then(result.type).isEqualTo("temperature")
        then(result.mac).isEqualTo("AA:BB:CC:DD:EE:FF")
        then(result.active).isTrue
        then(result.createdAt).isEqualTo(FIXED_TIME)
        then(result.group!!.id).isEqualTo(group.id)
    }

    @Test
    fun `should return null when device id is unknown`() {
        val result = transaction { repository.findById(9999) }

        then(result).isNull()
    }

    @Test
    fun `should return device when found by mac`() {
        val group = savedGroup()
        transaction { repository.save(device("sensor-01", group)) }

        val result = transaction { repository.findByMac("AA:BB:CC:DD:EE:FF") }

        then(result).isNotNull
        then(result!!.name).isEqualTo("sensor-01")
    }

    @Test
    fun `should return null when mac is unknown`() {
        val result = transaction { repository.findByMac("FF:FF:FF:FF:FF:FF") }

        then(result).isNull()
    }

    @Test
    fun `should return device when name and active match`() {
        val group = savedGroup()
        transaction { repository.save(device("sensor-01", group, active = true)) }

        val result = transaction { repository.findByNameAndActive("sensor-01", true) }

        then(result).isNotNull
        then(result!!.name).isEqualTo("sensor-01")
    }

    @Test
    fun `should return null when active does not match`() {
        val group = savedGroup()
        transaction { repository.save(device("sensor-01", group, active = true)) }

        val result = transaction { repository.findByNameAndActive("sensor-01", false) }

        then(result).isNull()
    }

    @Test
    fun `should return null when name does not exist`() {
        val result = transaction { repository.findByNameAndActive("nonexistent", true) }

        then(result).isNull()
    }

    @Test
    fun `should return count of devices in group`() {
        val group1 = savedGroup("Group 1")
        val group2 = savedGroup("Group 2")
        transaction {
            repository.save(device("device-a", group1))
            repository.save(device("device-b", group1))
            repository.save(device("device-c", group2))
        }

        val count1 = transaction { repository.countByGroupId(group1.id!!) }
        val count2 = transaction { repository.countByGroupId(group2.id!!) }

        then(count1).isEqualTo(2)
        then(count2).isEqualTo(1)
    }

    @Test
    fun `should return zero for group with no devices`() {
        val group = savedGroup()

        val count = transaction { repository.countByGroupId(group.id!!) }

        then(count).isEqualTo(0)
    }

    @Test
    fun `should insert new device and assign id`() {
        val group = savedGroup()
        val newDevice = device("new-sensor", group)

        transaction { repository.save(newDevice) }

        then(newDevice.id).isNotNull.isGreaterThan(0)
        val found = transaction { repository.findById(newDevice.id!!) }
        then(found).isNotNull
        then(found!!.name).isEqualTo("new-sensor")
        then(found.createdAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `should update existing device`() {
        val group = savedGroup()
        val saved = transaction {
            repository.save(device("old-name", group, active = true))
            repository.findByNameAndActive("old-name", true)!!
        }

        transaction {
            saved.name = "new-name"
            saved.active = false
            saved.description = "Updated"
            repository.save(saved)
        }

        val updated = transaction { repository.findById(saved.id!!) }
        then(updated!!.name).isEqualTo("new-name")
        then(updated.active).isFalse
        then(updated.description).isEqualTo("Updated")
    }

    private fun savedGroup(name: String = "Test Group"): Group = transaction {
        Group().apply {
            this.name = name
            description = "desc"
            createdAt = FIXED_TIME
        }.also { groupRepository.save(it) }
    }

    private fun device(name: String, group: Group, active: Boolean = true) = Device(group).apply {
        this.name = name
        description = "Test sensor"
        type = "temperature"
        mac = "AA:BB:CC:DD:EE:FF"
        createdAt = FIXED_TIME
        this.active = active
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2024-01-15T10:00:00Z")
    }
}
