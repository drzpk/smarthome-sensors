package dev.drzepka.smarthome.sensors.server.infrastructure.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import dev.drzepka.smarthome.sensors.server.domain.entity.Measurement
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager

class InfluxDBMeasurementRepository(private val influxDbManager: InfluxDBDatabaseManager) : MeasurementRepository {

    private val log by Logger()

    override suspend fun save(groupId: Int, measurements: Collection<Measurement>) {
        log.debug("Saving {} measurements from group {}", measurements.size, groupId)

        val points = measurements.map { convertToPoint(it) }
        if (points.isEmpty())
            return

        influxDbManager.getInfluxDBClient(groupId)
            .getWriteKotlinApi()
            .writePoints(points, bucket = "temperature")
    }

    private fun convertToPoint(measurement: Measurement): Point {
        val point = Point(TEMPERATURE_MEASUREMENT)
        point.time(measurement.createdAt, WritePrecision.S)

        point.addTag(TEMPERATURE_MEASUREMENT_TAG_DEVICE, measurement.deviceId.toString())
        point.addTag(TEMPERATURE_MEASUREMENT_TAG_LOGGER, measurement.loggerId.toString())

        point.addField(TEMPERATURE_MEASUREMENT_FIELD_TEMPERATURE, measurement.temperature)
        point.addField(TEMPERATURE_MEASUREMENT_FIELD_HUMIDITY, measurement.humidity)
        point.addField(TEMPERATURE_MEASUREMENT_FIELD_BATTERY_VOLTAGE, measurement.batteryVoltage)
        point.addField(TEMPERATURE_MEASUREMENT_FIELD_BATTERY_LEVEL, measurement.batteryLevel)

        return point
    }

    companion object {
        private const val TEMPERATURE_MEASUREMENT = "temperature"
        private const val TEMPERATURE_MEASUREMENT_TAG_DEVICE = "device"
        private const val TEMPERATURE_MEASUREMENT_TAG_LOGGER = "logger"
        private const val TEMPERATURE_MEASUREMENT_FIELD_TEMPERATURE = "temperature"
        private const val TEMPERATURE_MEASUREMENT_FIELD_HUMIDITY = "humidity"
        private const val TEMPERATURE_MEASUREMENT_FIELD_BATTERY_VOLTAGE = "battery_voltage"
        private const val TEMPERATURE_MEASUREMENT_FIELD_BATTERY_LEVEL = "battery_level"
    }
}