package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.LoggerPrincipal
import dev.drzepka.smarthome.sensors.server.application.configuration.MEASUREMENTS_AUTH
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.service.MeasurementService
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.ktor.ext.get

fun Route.measurementController() {

    val measurementService = get<MeasurementService>()

    route("/measurements") {
        authenticate(MEASUREMENTS_AUTH) {
            post {
                val request = call.receive<CreateMeasurementsRequest>()
                val principal = call.authentication.principal<LoggerPrincipal>()!!
                val status = newSuspendedTransaction {
                    measurementService.createMeasurements(request, principal.logger)
                }

                call.respond(status)
            }
        }
    }
}
