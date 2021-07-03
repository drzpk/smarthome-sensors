package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import io.ktor.config.*

@Suppress("LeakingThis")
@Mockable
class InfluxDatabaseInitializer(config: ApplicationConfig) {

    private val log by Logger()

    lateinit var influxDBClient: InfluxDBClientKotlin

    init {
        log.info("Initializing InfluxDB connection")
        val url = config.property(INFLUXDB_URL).getString()
        val org = config.property(INFLUXDB_ORG).getString()
        val token = config.property(INFLUXDB_TOKEN).getString()

        influxDBClient = InfluxDBClientKotlinFactory.create(url, token.toCharArray(), org)
    }

    companion object {
        private const val INFLUXDB_URL = "database.influxdb.url"
        private const val INFLUXDB_ORG = "database.influxdb.org"
        private const val INFLUXDB_TOKEN = "database.influxdb.token"
    }
}