package dev.drzepka.smarthome.sensors.server

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dev.drzepka.smarthome.sensors.server.application.configuration.setupRouting
import dev.drzepka.smarthome.sensors.server.application.configuration.setupSecurity
import dev.drzepka.smarthome.sensors.server.application.configuration.setupStatusPages
import dev.drzepka.smarthome.sensors.server.application.configuration.sensorsServerKoinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.sessions.*
import org.koin.ktor.ext.Koin

fun Application.sensorsServer() {
    install(ContentNegotiation) {
        jackson {
            enable(JsonParser.Feature.ALLOW_COMMENTS)
            registerModule(JavaTimeModule())
        }
    }

    install(Sessions) {

    }

    install(Koin) {
        modules(sensorsServerKoinModule())
    }

    setupSecurity()
    setupRouting()
    setupStatusPages()
}