package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.presentation.deviceController
import dev.drzepka.smarthome.sensors.server.presentation.loggerController
import dev.drzepka.smarthome.sensors.server.presentation.measurementController
import io.ktor.application.*
import io.ktor.routing.*

fun Application.setupRouting() {

    routing {
        route("/api") {
            deviceController()
            measurementController()
            loggerController()
        }
    }
}