package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.LoggerPrincipal
import dev.drzepka.smarthome.sensors.server.application.configuration.MEASUREMENTS_AUTH
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.CreateMeasurementsRequestV2
import dev.drzepka.smarthome.sensors.server.application.service.MeasurementService
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Route.measurementControllerV2() {

    val measurementService = get<MeasurementService>()

    route("/v2/measurements") {
        authenticate(MEASUREMENTS_AUTH) {
            post {
                val request = call.receive<CreateMeasurementsRequestV2>()
                val principal = call.authentication.principal<LoggerPrincipal>()!!
                val status = transaction {
                    measurementService.createMeasurements(request, principal.logger)
                }

                call.respond(status)
            }
        }
    }
}
