package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import dev.drzepka.smarthome.sensors.server.domain.util.Mockable
import kotlinx.coroutines.*
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil

@Mockable
class TaskScheduler(private val clock: Clock = Clock.systemUTC()) {
    private val log by Logger()
    private val activeTasks = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Synchronized
    fun schedule(name: String, interval: Duration, task: (suspend () -> Unit)) {
        if (isActive(name))
            throw IllegalArgumentException("Task '$name' already scheduled")

        log.info("Scheduling task '{}'", name)
        activeTasks.add(name)
        startTask(name, interval, task)
    }

    @Synchronized
    fun cancel(name: String) {
        log.info("Cancelling task '{}'", name)
        val removed = activeTasks.remove(name)
        if (!removed)
            log.warn("No scheduled task '{}' was found", name)
    }

    private fun startTask(name: String, interval: Duration, task: (suspend () -> Unit)) {
        scope.launch {
            delay(getInitialDelay(interval))

            var nextPlannedExecution = clock.instant()
            while (isActive(name)) {
                log.info("Executing task '{}'", name)

                execute(name, task)

                nextPlannedExecution = nextPlannedExecution.plus(interval)
                val now = clock.instant()
                var millis = nextPlannedExecution.toEpochMilli() - now.toEpochMilli()

                if (millis < 1) {
                    val multiplicand = ceil(abs(millis) / interval.toMillis().toFloat()).toLong()
                    log.warn(
                        "Execution of task '{}' took longer than it's interval, skipping, next {} intervals",
                        name, multiplicand
                    )

                    nextPlannedExecution = nextPlannedExecution.plus(interval.multipliedBy(multiplicand))
                    millis = nextPlannedExecution.toEpochMilli() - now.toEpochMilli()
                }

                delay(millis)
            }
        }
    }

    private fun isActive(name: String): Boolean = activeTasks.contains(name)

    internal fun getInitialDelay(interval: Duration): Long {
        val now = clock.millis()
        val intervalMillis = interval.toMillis()
        val nextBoundary = (now / intervalMillis + 1) * intervalMillis
        return nextBoundary - now
    }

    private suspend fun execute(name: String, task: (suspend () -> Unit)) {
        try {
            task.invoke()
        } catch (e: Exception) {
            log.error("Error while executing task '{}'", name, e)
        }
    }
}