package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementFactory
import dev.drzepka.smarthome.sensors.server.application.service.*
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.LoggerRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDatabaseInitializer
import dev.drzepka.smarthome.sensors.server.infrastructure.database.SQLDatabaseInitializer
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedDeviceRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedLoggerRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.InfluxDBMeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.service.PBKDF2HashService
import io.ktor.application.*
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.sensorsServerKoinModule(): Module = module {

    // Application
    single { DeviceService(get()) }
    single { MeasurementService(get(), get(), get(), get()) }
    single { LoggerService(get(), get(), get()) }
    single { PasswordGeneratorService(get()) }
    single { TaskScheduler() }
    single { MeasurementFactory(get()) }

    // Infrastructure
    val sqlDatabaseInitializer = SQLDatabaseInitializer(environment.config)
    val influxDatabaseInitializer = InfluxDatabaseInitializer(environment.config)

    single { sqlDatabaseInitializer }
    single { influxDatabaseInitializer }
    single { ConfigurationProviderService(environment.config) }
    single<HashService> { PBKDF2HashService() }

    single<DeviceRepository> { ExposedDeviceRepository() }
    single<LoggerRepository> { ExposedLoggerRepository() }
    single<MeasurementRepository> { InfluxDBMeasurementRepository(get()) }
}