package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.FluxRecord
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager
import kotlinx.coroutines.channels.consumeEach
import java.time.Instant

class InfluxDBMeasurementRepository(private val influxDbManager: InfluxDBDatabaseManager) : MeasurementRepository {

    private val log by Logger()

    override suspend fun save(groupId: Int, measurements: Collection<Measurement>) {
        log.debug("Saving {} measurements from group {}", measurements.size, groupId)

        val points = measurements.map { convertToPoint(it) }
        if (points.isEmpty())
            return

        influxDbManager.getInfluxDBClient(groupId)
            .getWriteKotlinApi()
            .writePoints(points, bucket = "measurements")
    }

    override suspend fun findLatestMeasurementTime(deviceId: Int, since: Instant): Instant? {
        val query = """
            from(bucket: "measurements")
              |> range(start: $since)
              |> filter(fn: (r) => r["device"] == "$deviceId")
              |> group()
              |> last()
        """.trimIndent()

        var latest: Instant? = null
        for (client in influxDbManager.getAllInfluxDBClients()) {
            client.getQueryKotlinApi().query(query).consumeEach { record: FluxRecord ->
                val t = record.getTime() ?: return@consumeEach
                if (latest == null || t.isAfter(latest!!)) {
                    latest = t
                }
            }
        }
        return latest
    }

    private fun convertToPoint(measurement: Measurement): Point {
        val point = Point(measurement.type)
        point.time(measurement.createdAt, WritePrecision.S)

        point.addTag(TAG_DEVICE, measurement.deviceId.toString())
        point.addTag(TAG_LOGGER, measurement.loggerId.toString())

        measurement.fields.forEach { (name, value) ->
            if (value != null) point.addField(name, value)
        }

        return point
    }

    companion object {
        private const val TAG_DEVICE = "device"
        private const val TAG_LOGGER = "logger"
    }
}
