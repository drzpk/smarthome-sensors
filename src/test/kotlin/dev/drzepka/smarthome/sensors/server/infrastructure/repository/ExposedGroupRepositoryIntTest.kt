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
    fun `should return all saved groups`() {
        transaction {
            repository.save(group("Alpha"))
            repository.save(group("Beta"))
            repository.save(group("Gamma"))
        }

        val result = transaction { repository.findAll() }

        then(result).hasSize(3)
        then(result.map { it.name }).containsExactlyInAnyOrder("Alpha", "Beta", "Gamma")
    }

    @Test
    fun `should return group when found by id`() {
        val saved = transaction { repository.save(group("Sensors")); repository.findByName("Sensors")!! }

        val result = transaction { repository.findById(saved.id!!) }

        then(result).isNotNull
        then(result!!.id).isEqualTo(saved.id)
        then(result.name).isEqualTo("Sensors")
        then(result.description).isEqualTo("Test description")
    }

    @Test
    fun `should return null when group id is unknown`() {
        val result = transaction { repository.findById(9999) }

        then(result).isNull()
    }

    @Test
    fun `should return group when found by name`() {
        transaction { repository.save(group("Living Room")) }

        val result = transaction { repository.findByName("Living Room") }

        then(result).isNotNull
        then(result!!.name).isEqualTo("Living Room")
    }

    @Test
    fun `should return null when group name does not exist`() {
        val result = transaction { repository.findByName("Nonexistent") }

        then(result).isNull()
    }

    @Test
    fun `should insert new group and assign id`() {
        val newGroup = group("Garage")

        transaction { repository.save(newGroup) }

        then(newGroup.id).isNotNull.isGreaterThan(0)
        val found = transaction { repository.findById(newGroup.id!!) }
        then(found).isNotNull
        then(found!!.name).isEqualTo("Garage")
        then(found.description).isEqualTo("Test description")
        then(found.createdAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `should update existing group`() {
        val saved = transaction { repository.save(group("Old Name")); repository.findByName("Old Name")!! }

        transaction {
            saved.name = "New Name"
            saved.description = "Updated description"
            repository.save(saved)
        }

        val updated = transaction { repository.findById(saved.id!!) }
        then(updated!!.name).isEqualTo("New Name")
        then(updated.description).isEqualTo("Updated description")
    }

    @Test
    fun `should remove group from database`() {
        val saved = transaction { repository.save(group("ToDelete")); repository.findByName("ToDelete")!! }

        transaction { repository.delete(saved.id!!) }

        val found = transaction { repository.findById(saved.id!!) }
        then(found).isNull()
    }

    @Test
    fun `should only remove the specified group`() {
        val toDelete = transaction { repository.save(group("ToDelete")); repository.findByName("ToDelete")!! }
        transaction { repository.save(group("ToKeep")) }

        transaction { repository.delete(toDelete.id!!) }

        val remaining = transaction { repository.findAll() }
        then(remaining).hasSize(1)
        then(remaining.first().name).isEqualTo("ToKeep")
    }

    private fun group(name: String) = Group().apply {
        this.name = name
        description = "Test description"
        createdAt = FIXED_TIME
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2024-01-15T10:00:00Z")
    }
}
