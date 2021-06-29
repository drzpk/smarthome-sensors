package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.dto.device.CreateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.dto.device.UpdateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.service.DeviceService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Route.deviceController() {

    val deviceService = get<DeviceService>()

    route("/devices") {
        post("") {
            val request = call.receive<CreateDeviceRequest>()
            val resource = transaction {
                deviceService.createDevice(request)
            }

            call.respond(resource)
        }

        get("") {
            val list = transaction {
                deviceService.getDevices()
            }

            call.respond(list)
        }

        get("/{id}") {
            val deviceId = call.parameters["id"]!!.toInt()
            val resource = transaction {
                deviceService.getDevice(deviceId)
            }

            call.respond(resource)
        }

        patch("/{id}") {
            val deviceId = call.parameters["id"]!!.toInt()
            val request = call.receive<UpdateDeviceRequest>()
            request.id = deviceId

            val resource = transaction {
                deviceService.updateDevice(request)
            }

            call.respond(resource)
        }

        delete("/{id}") {
            val deviceId = call.parameters["id"]!!.toInt()
            transaction {
                deviceService.deleteDevice(deviceId)
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}