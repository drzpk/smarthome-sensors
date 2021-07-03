package dev.drzepka.smarthome.sensors.server.application.util

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LifespanTracker<T>(private val lifespan: Duration) {
    private val map = TreeMap<Instant, T>()
    private val lock = ReentrantReadWriteLock()

    fun track(time: Instant, obj: T, now: Instant = Instant.now()) {
        lock.write {
            clearOldData(now)
            map.put(time, obj)
        }
    }

    fun exists(obj: T, now: Instant = Instant.now()): Boolean {
        return lock.read {
            val found = map.entries.firstOrNull { it.value == obj }
            found != null && isValid(found.key, now)
        }
    }

    private fun clearOldData(now: Instant) {
        val it = map.iterator()

        while (it.hasNext()) {
            val next = it.next()
            if (!isValid(next.key, now))
                it.remove()
            else
                break
        }
    }

    private fun isValid(time: Instant, now: Instant): Boolean = !time.plus(lifespan).isBefore(now)
}