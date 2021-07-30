package dev.drzepka.smarthome.sensors.server.domain.entity

import java.time.Instant

class Group : Entity<Int>() {
    var name = ""
    var description = ""
    var createdAt: Instant = Instant.now()
}