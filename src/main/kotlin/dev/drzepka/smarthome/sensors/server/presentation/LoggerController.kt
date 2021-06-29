package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.dto.logger.CreateLoggerRequest
import dev.drzepka.smarthome.sensors.server.application.dto.logger.UpdateLoggerRequest
import dev.drzepka.smarthome.sensors.server.application.service.LoggerService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Route.loggerController() {
    val loggerService = get<LoggerService>()

    route("/loggers") {
        get("") {
            val loggers = transaction {
                loggerService.listLoggers()
            }

            call.respond(loggers)
        }

        get("/{loggerId}") {
            val loggerId = call.parameters["loggerId"]!!.toInt()

            val resource = transaction {
                loggerService.getLogger(loggerId)
            }

            call.respond(resource)
        }

        post("") {
            val request = call.receive<CreateLoggerRequest>()

            val resource = transaction {
                loggerService.createLogger(request)
            }

            call.respond(HttpStatusCode.Created, resource)
        }

        patch("/{loggerId}") {
            val loggerId = call.parameters["loggerId"]!!.toInt()
            val request = call.receive<UpdateLoggerRequest>()
            request.id = loggerId

            val resource = transaction {
                loggerService.updateLogger(request)
            }

            call.respond(resource)
        }

        delete("/{loggerId}") {
            val loggerId = call.parameters["loggerId"]!!.toInt()

            transaction {
                loggerService.deleteLogger(loggerId)
            }

            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{loggerId}/password") {
            val loggerId = call.parameters["loggerId"]!!.toInt()

            val resource = transaction {
                loggerService.resetPassword(loggerId)
            }

            call.respond(resource)
        }
    }
}