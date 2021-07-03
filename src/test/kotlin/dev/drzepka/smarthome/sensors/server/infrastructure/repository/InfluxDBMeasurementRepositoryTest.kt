package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.WriteKotlinApi
import com.influxdb.client.write.Point
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDatabaseInitializer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class InfluxDBMeasurementRepositoryTest {

    private val writeApi = mock<WriteKotlinApi>()

    @Test
    fun `should save measurements`() = runBlocking {
        val measurement = Measurement(Instant.now(), 1, 2)
        measurement.temperature = BigDecimal("23.12")
        measurement.humidity = BigDecimal("70.40")
        measurement.batteryVoltage = BigDecimal("3.122")
        measurement.batteryLevel = 92

        getRepository().save(listOf(measurement))

        val captor = argumentCaptor<List<Point>>()
        verify(writeApi).writePoints(captor.capture(), any(), eq(null))

        val points = captor.firstValue
        then(points).hasSize(1)

        val point = points.first()
        val tags = getField<Map<String, String>>(point, "tags")
        val fields = getField<Map<String, Any>>(point, "fields")

        val expectedTags = mapOf(
            "device" to "1",
            "logger" to "2"
        )
        then(tags).containsExactlyInAnyOrderEntriesOf(expectedTags)

        val expectedFields = mapOf(
            "temperature" to BigDecimal("23.12"),
            "humidity" to BigDecimal("70.40"),
            "battery_voltage" to BigDecimal("3.122"),
            "battery_level" to 92
        )
        then(fields).containsExactlyInAnyOrderEntriesOf(expectedFields)

        Unit
    }

    private fun getRepository(): InfluxDBMeasurementRepository {
        val influxDBClient = mock<InfluxDBClientKotlin> {
            on { getWriteKotlinApi() }.doReturn(writeApi)
        }

        val initializer = mock<InfluxDatabaseInitializer> {
            on { this.influxDBClient }.doReturn(influxDBClient)
        }

        return InfluxDBMeasurementRepository(initializer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(obj: Any, fieldName: String): T {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj) as T
    }
}