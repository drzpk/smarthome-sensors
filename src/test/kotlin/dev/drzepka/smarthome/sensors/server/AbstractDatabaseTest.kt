package dev.drzepka.smarthome.sensors.server

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@Suppress("MemberVisibilityCanBePrivate")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDatabaseTest {

    open val recreateDatabaseBeforeEach = true
    open val tables: Array<out Table> = emptyArray()

    protected val database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;IGNORECASE=true;")

    @BeforeEach
    private fun setupBeforeEach() {
        if (recreateDatabaseBeforeEach)
            setupDatabse()
    }

    @BeforeAll
    private fun setupBeforeAll() {
        if (!recreateDatabaseBeforeEach)
            setupDatabse()
    }

    private fun setupDatabse() {
        transaction {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    @AfterAll
    private fun clearDatabase() {
        TransactionManager.closeAndUnregister(database)
    }
}