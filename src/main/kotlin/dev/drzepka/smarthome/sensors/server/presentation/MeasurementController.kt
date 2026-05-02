package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.LoggerPrincipal
import dev.drzepka.smarthome.sensors.server.application.configuration.MEASUREMENTS_AUTH
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.CreateMeasurementsRequest
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.CreateMeasurementsRequestV2
import dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2.TemperatureMeasurement
import java.time.Instant
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

fun Route.measurementController() {

    val measurementService = get<MeasurementService>()

    route("/measurements") {
        authenticate(MEASUREMENTS_AUTH) {
            post {
                val request = call.receive<CreateMeasurementsRequest>()
                val principal = call.authentication.principal<LoggerPrincipal>()!!
                val status = transaction {
                    measurementService.createMeasurements(request.toV2(), principal.logger)
                }

                call.respond(status)
            }
        }
    }
}

private fun CreateMeasurementsRequest.toV2(): CreateMeasurementsRequestV2 {
    val now = Instant.now()
    val v2 = CreateMeasurementsRequestV2()
    v2.measurements = measurements.mapTo(ArrayList()) { m ->
        TemperatureMeasurement(
            deviceId = m.deviceId,
            time = now.minusMillis(m.timestampOffsetMillis),
            temperature = m.temperature,
            humidity = m.humidity,
            batteryVoltage = m.batteryVoltage,
            batteryLevel = m.batteryLevel
        )
    }
    return v2
}
