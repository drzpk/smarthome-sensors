package dev.drzepka.smarthome.sensors.server.application.service

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

internal class ConfigurationProviderServiceTest {

    @Test
    fun `should load and use external config file`() {
        val service = getService()

        then(service.getOptionalInt("prop.someInteger")).isEqualTo(100)
        then(service.getOptionalInt("prop.anotherInteger")).isEqualTo(2)
        then(service.getOptionalInt("yetAnotherInteger")).isEqualTo(40)
        then(service.getOptionalInt("non.existent.prop")).isNull()
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