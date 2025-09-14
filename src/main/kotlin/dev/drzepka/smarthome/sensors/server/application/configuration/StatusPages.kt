package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.handler.ValidationExceptionHandler
import dev.drzepka.smarthome.sensors.server.domain.exception.NotFoundException
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

fun Application.setupStatusPages() {
    install(StatusPages) {
        val log = LoggerFactory.getLogger("ExceptionHandler")

        val validationExceptionHandler = ValidationExceptionHandler()

        exception<ValidationException> { call, cause ->
            val result = validationExceptionHandler.handle(cause)
            if (result.body != null)
                call.respond(result.statusCode, result.body)
            else
                call.respond(result.statusCode)
        }

        exception<NotFoundException> { call, cause ->
            if (cause.message != null)
                call.respond(HttpStatusCode.NotFound, ErrorDetails(cause.message!!))
            else
                call.respond(HttpStatusCode.NotFound)
        }

        exception<Exception> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private data class ErrorDetails(val message: String)