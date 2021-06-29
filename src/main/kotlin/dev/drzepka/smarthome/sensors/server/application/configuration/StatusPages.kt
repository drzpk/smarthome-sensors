package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.handler.ValidationExceptionHandler
import dev.drzepka.smarthome.sensors.server.domain.exception.NotFoundException
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun Application.setupStatusPages() {
    install(StatusPages) {

        val validationExceptionHandler = ValidationExceptionHandler()

        exception<ValidationException> { cause ->
            val result = validationExceptionHandler.handle(cause)
            if (result.body != null)
                call.respond(result.statusCode, result.body)
            else
                call.respond(result.statusCode)
        }

        exception<NotFoundException> { cause ->
            if (cause.message != null)
                call.respond(HttpStatusCode.NotFound, ErrorDetails(cause.message))
            else
                call.respond(HttpStatusCode.NotFound)
        }
    }
}

private data class ErrorDetails(val message: String)