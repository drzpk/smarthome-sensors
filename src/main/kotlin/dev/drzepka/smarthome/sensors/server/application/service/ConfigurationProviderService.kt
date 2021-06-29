package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import io.ktor.config.*

@Mockable
class ConfigurationProviderService(private val config: ApplicationConfig) {

    fun getInt(path: String, default: Int? = null): Int {
        return getOptionalInt(path, default) ?: reportPropertyNotFound(path)
    }

    fun getOptionalInt(path: String, default: Int? = null): Int? {
        val property =config.propertyOrNull(path) ?: return null
        val value = property.getString()
        return value.toInt()
    }

    private fun reportPropertyNotFound(path: String): Nothing {
        throw IllegalStateException("Property '$path' wasn't found")
    }
}