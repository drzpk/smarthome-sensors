package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.typesafe.config.ConfigFactory
import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.assertj.core.api.BDDAssertions.thenIllegalArgumentException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class InfluxDBDatabaseManagerIntTest : BaseIntegrationTest() {

    @Test
    fun `should create working client for configured group`(): Unit = runBlocking {
        val manager = createManager(groups = listOf(1))

        val client = manager.getInfluxDBClient(1)
        val ping = client.ping()

        then(ping).isTrue
    }

    @Test
    fun `should map multiple groups to same client`(): Unit = runBlocking {
        val manager = createManager(groups = listOf(1, 2))

        val client1 = manager.getInfluxDBClient(1)
        val client2 = manager.getInfluxDBClient(2)
        val ping1 = client1.ping()
        val ping2 = client2.ping()

        then(ping1).isTrue
        then(ping2).isTrue
    }

    @Test
    fun `should throw for unknown group`() {
        val manager = createManager(groups = listOf(1))

        thenIllegalArgumentException()
            .isThrownBy { manager.getInfluxDBClient(999) }
            .withMessage("No InfluxDB client found for group 999")
    }

    private fun createManager(groups: List<Int>): InfluxDBDatabaseManager {
        val groupList = groups.joinToString(",")
        val config = ConfigFactory.parseString(
            """
            database {
              influxdb = [
                {
                  groups = [$groupList]
                  url = "${influxDB.url}"
                  org = "$INFLUX_ORG"
                  token = "$INFLUX_ADMIN_TOKEN"
                }
              ]
            }
            """.trimIndent()
        )
        val configProvider = mock<ConfigurationProviderService>()
        whenever(configProvider.config).thenReturn(config)
        return InfluxDBDatabaseManager(configProvider)
    }
}
