package dev.drzepka.smarthome.sensors.server

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dev.drzepka.smarthome.sensors.server.application.configuration.setupRouting
import dev.drzepka.smarthome.sensors.server.application.configuration.setupSecurity
import dev.drzepka.smarthome.sensors.server.application.configuration.setupStatusPages
import dev.drzepka.smarthome.sensors.server.application.configuration.sensorsServerKoinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.sessions.Sessions
import org.koin.ktor.plugin.Koin

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