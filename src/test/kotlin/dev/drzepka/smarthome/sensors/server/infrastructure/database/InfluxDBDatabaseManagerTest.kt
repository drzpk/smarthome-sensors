package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.typesafe.config.ConfigFactory
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import org.assertj.core.api.BDDAssertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
internal class InfluxDBDatabaseManagerTest {

    private val configProvider = mock<ConfigurationProviderService>()

    @Test
    fun `should get InfluxDB client`() {
        val config = ConfigFactory.parseString(CONFIG_TEXT)
        whenever(configProvider.config).thenReturn(config)
        val manager = InfluxDBDatabaseManager(configProvider)

        assertThatCode { manager.getInfluxDBClient(1) }.doesNotThrowAnyException()
        assertThatCode { manager.getInfluxDBClient(2) }.doesNotThrowAnyException()
        assertThatIllegalArgumentException().isThrownBy { manager.getInfluxDBClient(3) }
            .withMessage("No InfluxDB client found for group 3")
    }

    companion object {
        private val CONFIG_TEXT = """
            database {
              sql {
                jdbcUrl = "jdbc:mysql://localhost/smart_home_sensors"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                username = "smart_home_sensors"
                password = "smart_home_sensors"
                maximumPoolSize = 10
              }
            
              influxdb = [
                {
                  groups = [1,2]
                  url = "http://localhost:8086"
                  org = "drzepka.dev"
                  token = "Biz_jG-pjvRv8tpoloCqvidTpBXPf2dGB3sqg1Gr4mJTmSKm5khJblidJhAsPM79i_FIU4ATNE8HXuxFON1mMw=="
                }
              ]
            }
        """.trimMargin()
    }
}