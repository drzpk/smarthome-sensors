package dev.drzepka.smarthome.sensors.server.application.dto.measurement

class CreateMeasurementsResponse {
    var created = 0
    var duplicated = 0
    var errors = 0

    val total: Int
        get() = created + duplicated + errors
}