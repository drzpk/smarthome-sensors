package dev.drzepka.smarthome.sensors.server

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.drzepka.smarthome.sensors.server.application.configuration.setupRouting
import dev.drzepka.smarthome.sensors.server.application.configuration.setupSecurity
import dev.drzepka.smarthome.sensors.server.application.configuration.setupStatusPages
import dev.drzepka.smarthome.sensors.server.application.factory.*
import dev.drzepka.smarthome.sensors.server.application.service.*
import dev.drzepka.smarthome.sensors.server.domain.repository.*
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.*
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Devices
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Groups
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.table.Loggers
import dev.drzepka.smarthome.sensors.server.infrastructure.service.PBKDF2HashService
import io.ktor.client.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.testcontainers.containers.InfluxDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

abstract class BaseIntegrationTest {

    @BeforeEach
    fun resetDatabase() {
        TransactionManager.defaultDatabase = sqliteDatabase
        transaction(sqliteDatabase) {
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
        single { MeasurementService(get(), get(), get(), get(), get()) }
        single { LoggerService(get(), get(), get()) }
        single { GroupService(get(), get()) }
        single { PasswordGeneratorService(get()) }
        // TaskScheduler is mocked to prevent background coroutines from leaking between tests
        single { mock<TaskScheduler>() }
        single<List<MeasurementValidator<*>>>(named("validators")) { listOf(TemperatureMeasurementValidator(), PvMeasurementValidator()) }
        single<List<MeasurementFactory<*>>>(named("factories")) { listOf(TemperatureMeasurementFactory(), PvMeasurementFactory()) }
        single { MeasurementCommonValidator() }
        single { MeasurementCreator(deviceRepository = get(), commonValidator = get(), validators = get(named("validators")), factories = get(named("factories"))) }

        single(createdAtStart = true) { ConfigurationProviderService() }
        single<HashService> { PBKDF2HashService() }

        single<LiveDataRepository> { InMemoryLiveDataRepository() }
        single<DeviceRepository> { ExposedDeviceRepository(get()) }
        single<LoggerRepository> { ExposedLoggerRepository() }
        single<GroupRepository> { ExposedGroupRepository() }
        single<MeasurementRepository> { InfluxDBMeasurementRepository(createInfluxManager()) }
    }

    companion object {
        init {
            // Exposed converts Instant to LocalDateTime text for SQLite using the JVM timezone.
            // Setting UTC here ensures timestamps round-trip correctly in tests.
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        }

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

        val sqliteDatabase: Database by lazy {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:sqlite::memory:"
                driverClassName = "org.sqlite.JDBC"
                maximumPoolSize = 1
                connectionTestQuery = "SELECT 1"
            }
            Database.connect(HikariDataSource(config))
        }

        fun createInfluxClient(): InfluxDBClientKotlin =
            InfluxDBClientKotlinFactory.create(influxDB.url, INFLUX_ADMIN_TOKEN.toCharArray(), INFLUX_ORG)

        fun createInfluxManager(): InfluxDBDatabaseManager = mock {
            on { getInfluxDBClient(any()) } doReturn createInfluxClient()
        }
    }
}
