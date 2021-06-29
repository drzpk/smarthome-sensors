package dev.drzepka.smarthome.sensors.server.domain.entity

abstract class Entity<T> {
    open var id: T? = null

    fun isStored(): Boolean = id != null
}