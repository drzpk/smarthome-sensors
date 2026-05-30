package dev.drzepka.smarthome.sensors.server

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dev.drzepka.smarthome.sensors.server.application.configuration.sensorsServerKoinModule
import dev.drzepka.smarthome.sensors.server.application.configuration.setupRouting
import dev.drzepka.smarthome.sensors.server.application.configuration.setupSecurity
import dev.drzepka.smarthome.sensors.server.application.configuration.setupStatusPages
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun Application.sensorsServer() {
    install(Koin) {
        modules(sensorsServerKoinModule())
    }

    val config = get<ConfigurationProviderService>().config
    if (config.hasPath("requestLogging") && config.getBoolean("requestLogging")) {
        install(CallLogging) {
            level = Level.DEBUG
            logger = LoggerFactory.getLogger("REQUEST_LOGGER")
        }
    }

    install(ContentNegotiation) {
        jackson {
            enable(JsonParser.Feature.ALLOW_COMMENTS)
            enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
            registerModule(JavaTimeModule())
        }
    }

    install(Sessions)
    setupSecurity()
    setupRouting()
    setupStatusPages()
}
