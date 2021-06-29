package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.LoggerPrincipal
import dev.drzepka.smarthome.sensors.server.application.configuration.MEASUREMENTS_AUTH
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementRequest
import dev.drzepka.smarthome.sensors.server.application.service.MeasurementService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Route.measurementController() {

    val measurementService = get<MeasurementService>()

    route("/measurements") {
        authenticate(MEASUREMENTS_AUTH) {
            post {
                val request = call.receive<CreateMeasurementRequest>()
                val principal = call.authentication.principal<LoggerPrincipal>()!!
                val status = transaction {
                    measurementService.addMeasurement(request, principal.logger)
                }

                call.respond(if (status) HttpStatusCode.Created else HttpStatusCode.OK)
            }
        }
    }
}