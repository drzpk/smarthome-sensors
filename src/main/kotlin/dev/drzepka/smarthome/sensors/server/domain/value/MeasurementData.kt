package dev.drzepka.smarthome.sensors.server.domain.value

import java.math.BigDecimal
import java.time.Instant

class MeasurementData {
    var time: Instant = Instant.now()
    var temperature: BigDecimal = BigDecimal.ZERO
    var humidity: BigDecimal = BigDecimal.ZERO
    var batteryVoltage: BigDecimal = BigDecimal.ZERO
    var batteryLevel = 0
}