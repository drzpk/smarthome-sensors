package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.factory.MeasurementFactory
import dev.drzepka.smarthome.sensors.server.application.service.*
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.LoggerRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.MeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager
import dev.drzepka.smarthome.sensors.server.infrastructure.database.SQLDatabaseInitializer
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedDeviceRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedGroupRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.ExposedLoggerRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.InfluxDBMeasurementRepository
import dev.drzepka.smarthome.sensors.server.infrastructure.service.PBKDF2HashService
import org.koin.core.module.Module
import org.koin.dsl.module

fun sensorsServerKoinModule(): Module = module {

    // Application
    single { DeviceService(get(), get()) }
    single { MeasurementService(get(), get(), get(), get()) }
    single { LoggerService(get(), get(), get()) }
    single { GroupService(get(), get()) }
    single { PasswordGeneratorService(get()) }
    single { TaskScheduler() }
    single { MeasurementFactory(get()) }

    // Infrastructure
    single(createdAtStart = true) { SQLDatabaseInitializer(get()) }
    single(createdAtStart = true) { InfluxDBDatabaseManager(get()) }
    single(createdAtStart = true) { ConfigurationProviderService() }
    single<HashService> { PBKDF2HashService() }

    single<DeviceRepository> { ExposedDeviceRepository(get()) }
    single<LoggerRepository> { ExposedLoggerRepository() }
    single<MeasurementRepository> { InfluxDBMeasurementRepository(get()) }
    single<GroupRepository> { ExposedGroupRepository() }
}