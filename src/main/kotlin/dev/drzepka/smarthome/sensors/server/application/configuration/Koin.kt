package dev.drzepka.smarthome.sensors.server.application.configuration

import dev.drzepka.smarthome.sensors.server.application.factory.*
import dev.drzepka.smarthome.sensors.server.application.service.*
import dev.drzepka.smarthome.sensors.server.domain.repository.*
import dev.drzepka.smarthome.sensors.server.infrastructure.database.InfluxDBDatabaseManager
import dev.drzepka.smarthome.sensors.server.infrastructure.database.SQLDatabaseInitializer
import dev.drzepka.smarthome.sensors.server.infrastructure.repository.*
import dev.drzepka.smarthome.sensors.server.infrastructure.service.PBKDF2HashService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun sensorsServerKoinModule(): Module = module {

    // Application
    single { DeviceService(get(), get()) }
    single { MeasurementService(get(), get(), get(), get(), get()) }
    single { LoggerService(get(), get(), get()) }
    single { GroupService(get(), get()) }
    single { PasswordGeneratorService(get()) }
    single { TaskScheduler() }
    single<List<MeasurementValidator<*>>>(named("validators")) { listOf(TemperatureMeasurementValidator(), PvMeasurementValidator(), EnergyMeasurementValidator()) }
    single<List<MeasurementFactory<*>>>(named("factories")) { listOf(TemperatureMeasurementFactory(), PvMeasurementFactory(), EnergyMeasurementFactory()) }
    single { MeasurementCommonValidator() }
    single { MeasurementCreator(deviceRepository = get(), commonValidator = get(), validators = get(named("validators")), factories = get(named("factories"))) }

    // Infrastructure
    single(createdAtStart = true) { SQLDatabaseInitializer(get()) }
    single(createdAtStart = true) { InfluxDBDatabaseManager(get()) }
    single(createdAtStart = true) { ConfigurationProviderService() }
    single<HashService> { PBKDF2HashService() }

    single<LiveDataRepository> { InMemoryLiveDataRepository() }
    single<DeviceRepository> { ExposedDeviceRepository(get()) }
    single<LoggerRepository> { ExposedLoggerRepository() }
    single<MeasurementRepository> { InfluxDBMeasurementRepository(get()) }
    single<GroupRepository> { ExposedGroupRepository() }
}
