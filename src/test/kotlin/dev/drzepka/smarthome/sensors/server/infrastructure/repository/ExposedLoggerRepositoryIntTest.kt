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
    fun `should save and find logger by id`() {
        val logger = transaction { repository.save(newLogger("sensor-01")); repository.findByNameAndActive("sensor-01", true)!! }

        val found = transaction { repository.findById(logger.id!!) }

        then(found).isNotNull
        then(found!!.name).isEqualTo("sensor-01")
        then(found.description).isEqualTo("Test logger")
        then(found.active).isTrue
    }

    @Test
    fun `should find logger by name and active status`() {
        transaction {
            repository.save(newLogger("active-logger", active = true))
            repository.save(newLogger("inactive-logger", active = false))
        }

        val active = transaction { repository.findByNameAndActive("active-logger", true) }
        val inactive = transaction { repository.findByNameAndActive("inactive-logger", false) }
        val notFound = transaction { repository.findByNameAndActive("active-logger", false) }

        then(active).isNotNull
        then(inactive).isNotNull
        then(notFound).isNull()
    }

    @Test
    fun `should return only active loggers when filtered`() {
        transaction {
            repository.save(newLogger("active", active = true))
            repository.save(newLogger("inactive", active = false))
        }

        val activeOnly = transaction { repository.findAll(active = true) }

        then(activeOnly).hasSize(1)
        then(activeOnly.first().name).isEqualTo("active")
    }

    @Test
    fun `should update logger`() {
        val logger = transaction {
            repository.save(newLogger("original"))
            repository.findByNameAndActive("original", true)!!
        }

        transaction {
            logger.name = "updated"
            logger.active = false
            repository.save(logger)
        }

        val found = transaction { repository.findById(logger.id!!) }
        then(found!!.name).isEqualTo("updated")
        then(found.active).isFalse
    }

    private fun newLogger(name: String, active: Boolean = true) = Logger().apply {
        this.name = name
        description = "Test logger"
        password = "hashed-password"
        createdAt = Instant.now()
        this.active = active
    }
}
