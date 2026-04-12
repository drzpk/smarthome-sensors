package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.presentation.deviceController
import dev.drzepka.smarthome.sensors.server.presentation.groupController
import dev.drzepka.smarthome.sensors.server.presentation.loggerController
import dev.drzepka.smarthome.sensors.server.presentation.measurementController
import dev.drzepka.smarthome.sensors.server.presentation.measurementControllerV2
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.setupRouting() {

    routing {
        route("/api") {
            deviceController()
            groupController()
            measurementController()
            measurementControllerV2()
            loggerController()
        }
    }
}