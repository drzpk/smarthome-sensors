package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.typesafe.config.Config
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable

@Suppress("LeakingThis")
@Mockable
class InfluxDBDatabaseManager(configProvider: ConfigurationProviderService) {

    private val log by Logger()

    private val clientMap = HashMap<Int, InfluxDBClientKotlin>()

    init {
        val connectionsConfig = configProvider.config.getConfigList(INFLUXDB_CONNECTIONS)
        log.info("Initializing InfluxDB ${connectionsConfig.size} connections")

        for ((index, singleConfig) in connectionsConfig.withIndex()) {
            val groups = singleConfig.getIntList(INFLUXDB_GROUPS)
            log.debug("Creating InfluxDB connection for groups {}", groups)

            val created = createClient(index, singleConfig)
            groups.forEach { clientMap[it] = created }
        }
    }

    fun getInfluxDBClient(groupId: Int): InfluxDBClientKotlin {
        return clientMap[groupId] ?: throw IllegalArgumentException("No InfluxDB client found for group $groupId")
    }

    private fun createClient(index: Int, config: Config): InfluxDBClientKotlin {
        try {
            val url = config.getString(INFLUXDB_URL)
            val org = config.getString(INFLUXDB_ORG)
            val token = config.getString(INFLUXDB_TOKEN)
            return InfluxDBClientKotlinFactory.create(url, token.toCharArray(), org)
        } catch (e: Exception) {
            throw IllegalStateException("Error while creating InfluxDB client #$index", e)
        }
    }

    companion object {
        private const val INFLUXDB_CONNECTIONS = "database.influxdb"
        private const val INFLUXDB_GROUPS = "groups"
        private const val INFLUXDB_URL = "url"
        private const val INFLUXDB_ORG = "org"
        private const val INFLUXDB_TOKEN = "token"
    }
}