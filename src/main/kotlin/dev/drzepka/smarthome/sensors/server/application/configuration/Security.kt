package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.LoggerPrincipal
import dev.drzepka.smarthome.sensors.server.application.service.LoggerService
import io.ktor.application.*
import io.ktor.auth.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

const val MEASUREMENTS_AUTH = "measurements"

fun Application.setupSecurity() {

    install(Authentication) {
        basic(MEASUREMENTS_AUTH) {
            realm = "logger-measurements"
            validate { credentials ->
                val loggerService = get<LoggerService>()
                val logger = transaction { loggerService.getLogger(credentials.name.toInt(), credentials.password) }
                if (logger != null)
                    LoggerPrincipal(logger)
                else
                    null
            }
        }
    }
}