package dev.drzepka.smarthome.sensors.server.application.dto.measurement

import java.math.BigDecimal

class CreateMeasurementsRequest {

    var measurements = ArrayList<Measurement>()

    class Measurement {
        var deviceId = 0
        var temperature: BigDecimal = BigDecimal.ZERO
        var humidity: BigDecimal = BigDecimal.ZERO
        var batteryVoltage: BigDecimal = BigDecimal.ZERO
        var batteryLevel = 0
        var timestampOffsetMillis: Long = 0

        override fun toString(): String {
            return "Measurement(deviceId=$deviceId, temperature=$temperature, humidity=$humidity, " +
                    "batteryVoltage=$batteryVoltage, batteryLevel=$batteryLevel, " +
                    "timestampOffsetMillis=$timestampOffsetMillis)"
        }
    }
}