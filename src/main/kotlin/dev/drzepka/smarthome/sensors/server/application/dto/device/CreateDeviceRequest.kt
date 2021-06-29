package dev.drzepka.smarthome.sensors.server.application.dto.device

class CreateDeviceRequest : UpdateDeviceRequest() {
    var mac: String? = null
}