package dev.drzepka.smarthome.sensors.server.domain.entity

import java.math.BigDecimal
import java.time.Instant

class Measurement(var createdAt: Instant, var deviceId: Int, var loggerId: Int, var groupId: Int) {
    var temperature: BigDecimal = BigDecimal.ZERO
    var humidity: BigDecimal = BigDecimal.ZERO
    var batteryVoltage: BigDecimal = BigDecimal.ZERO
    var batteryLevel = 0
}