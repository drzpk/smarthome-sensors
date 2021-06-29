package dev.drzepka.smarthome.sensors.server.application.dto.device

open class UpdateDeviceRequest {
    var id = 0
    var name: String? = null
    var description: String? = null
}