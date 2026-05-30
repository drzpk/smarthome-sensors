package dev.drzepka.smarthome.sensors.server.application.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import java.io.File

@Suppress("LeakingThis")
@Mockable
class ConfigurationProviderService {

    final val config: Config

    private val log by Logger()

    init {
        val baseConfig = loadBaseConfiguration()
        val externalConfig = loadExternalConfiguration()

        config = if (externalConfig != null) {
            externalConfig.withFallback(baseConfig)
        } else {
            baseConfig
        }
    }

    internal fun loadBaseConfiguration(): Config {
        return ConfigFactory.load()
    }

    internal fun loadExternalConfiguration(): Config? {
        val path = System.getenv("CONFIG_FILE")
            ?: System.getProperty("CONFIG_FILE")
            ?: return null
        val file = File(path)
        if (!file.isFile)
            throw IllegalArgumentException("Configuration file $path doesn't exist")

        log.info("Loading external configuration file $path")
        return ConfigFactory.parseFile(file)
    }
}
