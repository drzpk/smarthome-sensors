package dev.drzepka.smarthome.sensors.server.application.service

import org.assertj.core.api.BDDAssertions.assertThatIllegalArgumentException
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Duration

class TaskSchedulerTest {

    @Test
    fun `should schedule task`() {
        val scheduler = TaskScheduler()

        var counter = 0
        scheduler.schedule("name", Duration.ofMillis(200)) {
            counter++
        }

        Thread.sleep(650)
        then(counter).isBetween(3, 5)
    }

    @Test
    fun `should cancel scheduled task`() {
        val scheduler = TaskScheduler()

        var counter = 0
        scheduler.schedule("name", Duration.ofMillis(100)) {
            counter++
        }

        Thread.sleep(220)
        val saved = counter
        scheduler.cancel("name")

        Thread.sleep(100)
        then(counter).isEqualTo(saved).isGreaterThan(0)
    }

    @Test
    fun `should throw exception on scheduling multiple tasks with the same name`() {
        val scheduler = TaskScheduler()

        scheduler.schedule("name", Duration.ofSeconds(1)) {}

        assertThatIllegalArgumentException().isThrownBy {
            scheduler.schedule("name", Duration.ofMinutes(1)) {}
        }.withMessage("Task 'name' already scheduled")
    }
}