package dev.drzepka.smarthome.sensors.server.domain.entity

import java.time.Instant

class Device : Entity<Int>() {
    var name = ""
    var description = ""
    var mac = ""
    var createdAt: Instant = Instant.now()
    var active = false
}