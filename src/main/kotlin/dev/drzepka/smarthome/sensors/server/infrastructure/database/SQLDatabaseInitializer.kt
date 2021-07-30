package dev.drzepka.smarthome.sensors.server.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.drzepka.smarthome.sensors.server.application.service.ConfigurationProviderService
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

class SQLDatabaseInitializer(config: ConfigurationProviderService) {

    private val log by Logger()

    init {
        val dataSource = getDataSource(config)

        log.info("Updating the database")
        val liquibaseDatabase =
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(dataSource.connection))
        val liquibase =
            Liquibase("classpath:/liquibase-changelog.xml", ClassLoaderResourceAccessor(), liquibaseDatabase)
        liquibase.update(Contexts(), LabelExpression())

        log.info("Creating the SQL database connection")
        Database.connect(dataSource)
    }

    private fun getDataSource(configurationProvider: ConfigurationProviderService): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = configurationProvider.config.getString(JDBC_URL)
            driverClassName = configurationProvider.config.getString(DRIVER_CLASS_NAME)
            username = configurationProvider.config.getString(USERNAME)
            password = configurationProvider.config.getString(PASSWORD)
            maximumPoolSize = configurationProvider.config.getInt(MAXIMUM_POOL_SIZE)
        }

        log.info("Creating datasource to database {} with user {}", hikariConfig.jdbcUrl, hikariConfig.username)
        return HikariDataSource(hikariConfig)
    }

    companion object {
        private const val JDBC_URL = "database.sql.jdbcUrl"
        private const val DRIVER_CLASS_NAME = "database.sql.driverClassName"
        private const val USERNAME = "database.sql.username"
        private const val PASSWORD = "database.sql.password"
        private const val MAXIMUM_POOL_SIZE = "database.sql.maximumPoolSize"
    }
}