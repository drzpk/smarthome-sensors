package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class InMemoryLiveDataRepositoryIntTest {

    private lateinit var repository: InMemoryLiveDataRepository

    @BeforeEach
    fun beforeEach() {
        repository = InMemoryLiveDataRepository()
    }

    @Test
    fun `should return null for device with no data`() {
        val result = repository.findByDeviceId(1)

        then(result).isNull()
    }

    @Test
    fun `should save and return live fields`() {
        val measurement = measurement(deviceId = 1, liveFields = mapOf("energy_today" to BigDecimal("12.5")))

        repository.save(measurement)

        then(repository.findByDeviceId(1)).isEqualTo(mapOf("energy_today" to BigDecimal("12.5")))
    }

    @Test
    fun `should not save measurement with empty live fields`() {
        repository.save(measurement(deviceId = 1, liveFields = emptyMap()))

        then(repository.findByDeviceId(1)).isNull()
    }

    @Test
    fun `should replace existing entry when newer measurement arrives`() {
        val older = measurement(deviceId = 1, time = T1, liveFields = mapOf("energy_today" to BigDecimal("10.0")))
        val newer = measurement(deviceId = 1, time = T2, liveFields = mapOf("energy_today" to BigDecimal("20.0")))

        repository.save(older)
        repository.save(newer)

        then(repository.findByDeviceId(1)!!["energy_today"]).isEqualTo(BigDecimal("20.0"))
    }

    @Test
    fun `should not replace existing entry when older measurement arrives`() {
        val newer = measurement(deviceId = 1, time = T2, liveFields = mapOf("energy_today" to BigDecimal("20.0")))
        val older = measurement(deviceId = 1, time = T1, liveFields = mapOf("energy_today" to BigDecimal("10.0")))

        repository.save(newer)
        repository.save(older)

        then(repository.findByDeviceId(1)!!["energy_today"]).isEqualTo(BigDecimal("20.0"))
    }

    @Test
    fun `should keep data for different devices separately`() {
        repository.save(measurement(deviceId = 1, liveFields = mapOf("energy_today" to BigDecimal("10.0"))))
        repository.save(measurement(deviceId = 2, liveFields = mapOf("energy_today" to BigDecimal("99.0"))))

        then(repository.findByDeviceId(1)!!["energy_today"]).isEqualTo(BigDecimal("10.0"))
        then(repository.findByDeviceId(2)!!["energy_today"]).isEqualTo(BigDecimal("99.0"))
        then(repository.findByDeviceId(3)).isNull()
    }

    private fun measurement(
        deviceId: Int,
        time: Instant = T1,
        liveFields: Map<String, Number> = mapOf("energy_today" to BigDecimal("12.5"))
    ) = Measurement(
        createdAt = time,
        deviceId = deviceId,
        loggerId = 1,
        groupId = 1,
        type = "pv",
        fields = emptyMap(),
        liveFields = liveFields
    )

    companion object {
        private val T1 = Instant.parse("2026-01-01T10:00:00Z")
        private val T2 = Instant.parse("2026-01-01T11:00:00Z")
    }
}
