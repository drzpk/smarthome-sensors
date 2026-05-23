package dev.drzepka.smarthome.sensors.server.application.util

import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LifespanTracker<T>(private val lifespan: Duration) {
    private val map = HashMap<T, Instant>()
    private val lock = ReentrantReadWriteLock()

    fun track(time: Instant, obj: T) {
        lock.write {
            val existing = map[obj]
            if (existing == null || time.isAfter(existing)) {
                map[obj] = time
            }
        }
    }

    fun isTracked(obj: T): Boolean = lock.read { map.containsKey(obj) }

    fun exists(time: Instant, obj: T): Boolean {
        return lock.read {
            val trackedTime = map[obj] ?: return@read false
            !trackedTime.plus(lifespan).isBefore(time)
        }
    }

    // Atomically checks existence and, if not a duplicate, updates the tracked time.
    // Returns true if the measurement at [time] is within the lifespan of the previously tracked time (duplicate).
    fun existsOrTrack(time: Instant, obj: T): Boolean {
        lock.write {
            val trackedTime = map[obj]
            if (trackedTime != null && !trackedTime.plus(lifespan).isBefore(time)) {
                return true
            }
            if (trackedTime == null || time.isAfter(trackedTime)) {
                map[obj] = time
            }
            return false
        }
    }
}
