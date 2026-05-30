package dev.drzepka.smarthome.sensors.server.application.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ConfigurationProviderServiceTest {

    @Test
    fun `should load and use external config file`() {
        val service = getService()

        then(service.config.getInt("prop.someInteger")).isEqualTo(100)
        then(service.config.getInt("prop.anotherInteger")).isEqualTo(2)
        then(service.config.getInt("yetAnotherInteger")).isEqualTo(40)
        assertThrows<ConfigException.Missing> {
            service.config.getInt("non.existent.prop")
        }
    }

    private fun getService(): ConfigurationProviderService = object : ConfigurationProviderService() {
        override fun loadBaseConfiguration(): Config = ConfigFactory.parseString(BASE_CONFIG)
        override fun loadExternalConfiguration(): Config = ConfigFactory.parseString(OVERRIDDEN_CONFIG)
    }

    companion object {
        private val BASE_CONFIG = """
            prop {
                someInteger = 1
                anotherInteger = 2
            }
        """.trimIndent()

        private val OVERRIDDEN_CONFIG = """
            prop {
                someInteger = 100
            }
            
            yetAnotherInteger = 40
        """.trimIndent()
    }
}