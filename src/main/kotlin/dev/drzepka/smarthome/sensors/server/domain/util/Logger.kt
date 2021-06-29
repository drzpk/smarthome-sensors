package dev.drzepka.smarthome.sensors.server.domain.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Logger : ReadOnlyProperty<Any, Logger> {

    private var logger: Logger? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): Logger {
        if (logger == null)
            logger = createLogger(thisRef::class.java)

        return logger!!
    }

    private fun createLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
}