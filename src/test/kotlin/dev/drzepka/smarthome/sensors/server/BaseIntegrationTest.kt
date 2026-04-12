package dev.drzepka.smarthome.sensors.server

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import dev.drzepka.smarthome.sensors.server.application.configuration.sensorsServerKoinModule
import dev.drzepka.smarthome.sensors.server.application.configuration.setupRouting
import dev.drzepka.smarthome.sensors.server.application.configuration.setupSecurity
import dev.drzepka.smarthome.sensors.server.application.configuration.setupStatusPages
import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementFactory
import dev.drzepka.smarthome.sensors.server.application.service.*
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.LoggerRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedDeviceRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedGroupRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedLoggerRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.InfluxDBMeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Devices
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Groups
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Loggers
import dev.drzepka.smarthome.sensors.server.infrastructure.service.PBKDF2HashService
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.utility.DockerImageName

abstract class BaseIntegrationTest {

    @BeforeEach
    fun resetDatabase() {
        TransactionManager.defaultDatabase = h2Database
        transaction(h2Database) {
            SchemaUtils.drop(Devices, Groups, Loggers)
            SchemaUtils.create(Groups, Loggers, Devices)
        }
    }

    protected fun testApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { setupTestApplication() }
        val client = createClient {
            install(ClientContentNegotiation) {
                jackson {
                    enable(JsonParser.Feature.ALLOW_COMMENTS)
                    registerModule(JavaTimeModule())
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
        }
        block(client)
    }

    private fun Application.setupTestApplication() {
        install(ServerContentNegotiation) {
            jackson {
                enable(JsonParser.Feature.ALLOW_COMMENTS)
                registerModule(JavaTimeModule())
            }
        }
        install(Sessions) {}
        install(Koin) {
            modules(testKoinModule())
        }
        setupSecurity()
        setupRouting()
        setupStatusPages()
    }

    private fun testKoinModule() = module {
        single { DeviceService(get(), get()) }
        single { MeasurementService(get(), get(), get(), get()) }
        single { LoggerService(get(), get(), get()) }
        single { GroupService(get(), get()) }
        single { PasswordGeneratorService(get()) }
        // TaskScheduler is mocked to prevent background coroutines from leaking between tests
        single { mock<TaskScheduler>() }
        single { MeasurementFactory(get()) }

        single(createdAtStart = true) { ConfigurationProviderService() }
        single<HashService> { PBKDF2HashService() }

        single<DeviceRepository> { ExposedDeviceRepository(get()) }
        single<LoggerRepository> { ExposedLoggerRepository() }
        single<GroupRepository> { ExposedGroupRepository() }
        single<MeasurementRepository> { InfluxDBMeasurementRepository(createInfluxManager()) }
    }

    companion object {
        const val INFLUX_ADMIN_TOKEN = "test-admin-token"
        const val INFLUX_ORG = "test-org"
        const val INFLUX_BUCKET = "measurements"

        val influxDB: InfluxDBContainer<*> by lazy {
            InfluxDBContainer(DockerImageName.parse("influxdb:2.7")).apply {
                withAdminToken(INFLUX_ADMIN_TOKEN)
                withOrganization(INFLUX_ORG)
                withBucket(INFLUX_BUCKET)
                start()
            }
        }

        val h2Database: Database by lazy {
            Database.connect(
                "jdbc:h2:mem:integration_test;DB_CLOSE_DELAY=-1;IGNORECASE=true",
                driver = "org.h2.Driver"
            )
        }

        fun createInfluxClient(): InfluxDBClientKotlin =
            InfluxDBClientKotlinFactory.create(influxDB.url, INFLUX_ADMIN_TOKEN.toCharArray(), INFLUX_ORG)

        fun createInfluxManager(): InfluxDBDatabaseManager = mock {
            on { getInfluxDBClient(any()) } doReturn createInfluxClient()
        }
    }
}
