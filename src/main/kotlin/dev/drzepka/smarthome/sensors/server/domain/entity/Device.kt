package dev.drzepka.smarthome.sensors.server.domain.entity

import java.time.Instant

class Device(var group: Group? = null) : Entity<Int>() {
    var name = ""
    var description = ""
    var type = ""
    var mac = ""
    var createdAt: Instant = Instant.now()
    var active = false
}