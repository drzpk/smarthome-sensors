package dev.drzepka.smarthome.sensors.server.domain.entity

import java.time.Instant

class Measurement(
    val createdAt: Instant,
    val deviceId: Int,
    val loggerId: Int,
    val groupId: Int,
    val type: String,
    val fields: Map<String, Number?>,
    val liveFields: Map<String, Number?> = emptyMap()
)
