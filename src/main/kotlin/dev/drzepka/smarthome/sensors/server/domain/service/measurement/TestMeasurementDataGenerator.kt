package dev.drzepka.smarthome.sensors.server.domain.service.measurement

import dev.drzepka.smarthome.sensors.server.domain.value.MeasurementData
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.*

class TestMeasurementDataGenerator(
    startTime: Instant,
    private val endTime: Instant,
    private val interval: Duration
) : Iterator<MeasurementData> {

    private val random = Random()
    private var currentTime = startTime

    override fun hasNext(): Boolean = currentTime.isBefore(endTime)

    override fun next(): MeasurementData {
        val measurement = createRandomMeasurement()
        currentTime = currentTime.plus(interval)
        return measurement
    }

    private fun createRandomMeasurement(): MeasurementData = MeasurementData().apply {
        time = currentTime
        temperature = BigDecimal.valueOf((random.nextInt(200) + 50) / 10.0).setScale(1, RoundingMode.FLOOR)
        humidity = BigDecimal.valueOf(random.nextInt(100) + 1L)
    }

}