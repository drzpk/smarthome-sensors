package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import org.assertj.core.api.BDDAssertions.then
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant

class ExposedLoggerRepositoryIntTest : BaseIntegrationTest() {

    private val repository = ExposedLoggerRepository()

    @Test
    fun `should return empty collection when no loggers exist`() {
        val result = transaction { repository.findAll() }

        then(result).isEmpty()
    }

    @Test
    fun `should return all loggers regardless of active status`() {
        transaction {
            repository.save(logger("logger-a", active = true))
            repository.save(logger("logger-b", active = false))
            repository.save(logger("logger-c", active = true))
        }

        val result = transaction { repository.findAll() }

        then(result).hasSize(3)
        then(result.map { it.name }).containsExactlyInAnyOrder("logger-a", "logger-b", "logger-c")
    }

    @Test
    fun `should return only active loggers when filtered by active`() {
        transaction {
            repository.save(logger("active", active = true))
            repository.save(logger("inactive", active = false))
        }

        val result = transaction { repository.findAll(active = true) }

        then(result).hasSize(1)
        then(result.first().name).isEqualTo("active")
    }

    @Test
    fun `should return only inactive loggers when filtered by inactive`() {
        transaction {
            repository.save(logger("active", active = true))
            repository.save(logger("inactive", active = false))
        }

        val result = transaction { repository.findAll(active = false) }

        then(result).hasSize(1)
        then(result.first().name).isEqualTo("inactive")
    }

    @Test
    fun `should return logger with all fields when found by id`() {
        val saved = transaction { repository.save(logger("sensor-01")); repository.findByNameAndActive("sensor-01", true)!! }

        val result = transaction { repository.findById(saved.id!!) }

        then(result).isNotNull
        then(result!!.id).isEqualTo(saved.id)
        then(result.name).isEqualTo("sensor-01")
        then(result.description).isEqualTo("Test logger")
        then(result.password).isEqualTo("hashed-password")
        then(result.active).isTrue
        then(result.createdAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `should return null when logger id is unknown`() {
        val result = transaction { repository.findById(9999) }

        then(result).isNull()
    }

    @Test
    fun `should return logger when name and active match`() {
        transaction { repository.save(logger("garage-sensor", active = true)) }

        val result = transaction { repository.findByNameAndActive("garage-sensor", true) }

        then(result).isNotNull
        then(result!!.name).isEqualTo("garage-sensor")
    }

    @Test
    fun `should return null when active does not match`() {
        transaction { repository.save(logger("garage-sensor", active = true)) }

        val result = transaction { repository.findByNameAndActive("garage-sensor", false) }

        then(result).isNull()
    }

    @Test
    fun `should return null when name does not exist`() {
        val result = transaction { repository.findByNameAndActive("nonexistent", true) }

        then(result).isNull()
    }

    @Test
    fun `should insert new logger and assign id`() {
        val newLogger = logger("new-logger")

        transaction { repository.save(newLogger) }

        then(newLogger.id).isNotNull.isGreaterThan(0)
        val found = transaction { repository.findById(newLogger.id!!) }
        then(found).isNotNull
        then(found!!.name).isEqualTo("new-logger")
        then(found.password).isEqualTo("hashed-password")
        then(found.createdAt).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `should update existing logger`() {
        val saved = transaction { repository.save(logger("original")); repository.findByNameAndActive("original", true)!! }

        transaction {
            saved.name = "updated"
            saved.description = "New description"
            saved.password = "new-hash"
            saved.active = false
            repository.save(saved)
        }

        val updated = transaction { repository.findById(saved.id!!) }
        then(updated!!.name).isEqualTo("updated")
        then(updated.description).isEqualTo("New description")
        then(updated.password).isEqualTo("new-hash")
        then(updated.active).isFalse
    }

    private fun logger(name: String, active: Boolean = true) = Logger().apply {
        this.name = name
        description = "Test logger"
        password = "hashed-password"
        createdAt = FIXED_TIME
        this.active = active
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2024-01-15T10:00:00Z")
    }
}
