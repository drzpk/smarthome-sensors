package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.typesafe.config.Config
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable

@Suppress("LeakingThis")
@Mockable
class InfluxDBDatabaseManager(private val configProvider: ConfigurationProviderService) {

    private val log by Logger()

    private val clientMap = HashMap<Int, InfluxDBClientKotlin>()
    private val allClients = mutableListOf<InfluxDBClientKotlin>()

    init {
        val connectionsConfig = configProvider.config.getConfigList(INFLUXDB_CONNECTIONS)
        log.info("Initializing InfluxDB ${connectionsConfig.size} connections")

        for ((index, singleConfig) in connectionsConfig.withIndex()) {
            val groups = singleConfig.getIntList(INFLUXDB_GROUPS)
            log.debug("Creating InfluxDB connection for groups {}", groups)

            val created = createClient(index, singleConfig)
            allClients.add(created)
            groups.forEach { clientMap[it] = created }
        }
    }

    fun getInfluxDBBucket(groupId: Int): String {
        val connectionsConfig = configProvider.config.getConfigList(INFLUXDB_CONNECTIONS)
        connectionsConfig.first {
            val groups = it.getIntList(INFLUXDB_GROUPS)
            groups.contains(groupId)
        }.let { return it.getString(INFLUXDB_BUCKET) }
    }

    fun getInfluxDBClient(groupId: Int): InfluxDBClientKotlin {
        return clientMap[groupId] ?: throw IllegalArgumentException("No InfluxDB client found for group $groupId")
    }

    private fun createClient(index: Int, config: Config): InfluxDBClientKotlin {
        try {
            val url = config.getString(INFLUXDB_URL)
            val org = config.getString(INFLUXDB_ORG)
            val token = config.getString(INFLUXDB_TOKEN)
            val bucket = config.getString(INFLUXDB_BUCKET)
            return InfluxDBClientKotlinFactory.create(url, token.toCharArray(), org, bucket)
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
        private const val INFLUXDB_BUCKET = "bucket"
    }
}