package dev.drzepka.smarthome.sensors.server.application.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import java.io.File

@Suppress("LeakingThis")
@Mockable
class ConfigurationProviderService {

    val config: Config

    private val log by Logger()

    init {
        val baseConfig = ConfigFactory.load()
        val externalConfigFile = getExternalConfigFile()

        config = if (externalConfigFile != null) {
            log.info("Loading external configuration file $externalConfigFile")
            ConfigFactory.parseFile(externalConfigFile).withFallback(baseConfig)
        } else {
            baseConfig
        }
    }

    fun getInt(path: String, default: Int? = null): Int {
        return getOptionalInt(path, default) ?: reportPropertyNotFound(path)
    }

    fun getOptionalInt(path: String, default: Int? = null): Int? {
        if (!config.hasPath(path))
            return null
        return config.getInt(path)
    }

    private fun getExternalConfigFile(): File? {
        val path = System.getProperty("EXTERNAL_CONFIG_PATH") ?: return null
        val file = File(path)
        if (!file.isFile)
            throw IllegalArgumentException("Configuration file $path doesn't exist")

        return file
    }

    private fun reportPropertyNotFound(path: String): Nothing {
        throw IllegalStateException("Property '$path' wasn't found")
    }
}