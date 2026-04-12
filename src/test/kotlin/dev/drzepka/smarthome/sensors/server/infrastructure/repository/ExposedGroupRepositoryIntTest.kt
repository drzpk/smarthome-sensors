package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import org.assertj.core.api.BDDAssertions.then
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant

class ExposedGroupRepositoryIntTest : BaseIntegrationTest() {

    private val repository = ExposedGroupRepository()

    @Test
    fun `should return empty list when no groups exist`() {
        val result = transaction { repository.findAll() }
        then(result).isEmpty()
    }

    @Test
    fun `should save and find group by id`() {
        val saved = transaction { repository.save(newGroup("Sensors")); repository.findByName("Sensors")!! }

        val found = transaction { repository.findById(saved.id!!) }

        then(found).isNotNull
        then(found!!.name).isEqualTo("Sensors")
        then(found.description).isEqualTo("A test group")
    }

    @Test
    fun `should find group by name`() {
        transaction { repository.save(newGroup("Living Room")) }

        val found = transaction { repository.findByName("Living Room") }

        then(found).isNotNull
        then(found!!.name).isEqualTo("Living Room")
    }

    @Test
    fun `should return null for unknown group`() {
        val found = transaction { repository.findById(9999) }
        then(found).isNull()
    }

    @Test
    fun `should update group`() {
        val group = transaction { repository.save(newGroup("Old Name")); repository.findByName("Old Name")!! }

        transaction {
            group.name = "New Name"
            repository.save(group)
        }

        val updated = transaction { repository.findById(group.id!!) }
        then(updated!!.name).isEqualTo("New Name")
    }

    @Test
    fun `should delete group`() {
        val group = transaction { repository.save(newGroup("ToDelete")); repository.findByName("ToDelete")!! }

        transaction { repository.delete(group.id!!) }

        val found = transaction { repository.findById(group.id!!) }
        then(found).isNull()
    }

    @Test
    fun `should return all groups`() {
        transaction {
            repository.save(newGroup("Group A"))
            repository.save(newGroup("Group B"))
        }

        val all = transaction { repository.findAll() }

        then(all).hasSize(2)
        then(all.map { it.name }).containsExactlyInAnyOrder("Group A", "Group B")
    }

    private fun newGroup(name: String) = Group().apply {
        this.name = name
        description = "A test group"
        createdAt = Instant.now()
    }
}
