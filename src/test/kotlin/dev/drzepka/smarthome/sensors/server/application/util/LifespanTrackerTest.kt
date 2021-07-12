package dev.drzepka.smarthome.sensors.server.application.util

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class LifespanTrackerTest {

    @Test
    fun `should track lifespan of one object`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        then(tracker.exists(now, TestItem(999))).isFalse

        tracker.track(now, TestItem(11))
        then(tracker.exists(now.plusSeconds(15), TestItem(11))).isTrue
        then(tracker.exists(now.plusSeconds(29), TestItem(11))).isTrue
        then(tracker.exists(now.plusSeconds(30), TestItem(11))).isTrue
        then(tracker.exists(now.plusSeconds(31), TestItem(11))).isFalse

        then(tracker.track(now.plusSeconds(31), TestItem(98), now = now.plusSeconds(31)))
        then(tracker.exists(now.plusSeconds(15), TestItem(11))).isFalse
    }

    @Test
    fun `should track lifespan of multiple objects`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        tracker.track(now, TestItem(1))
        tracker.track(now.plusSeconds(10), TestItem(2))
        tracker.track(now.plusSeconds(20), TestItem(3))

        then(tracker.exists(now.plusSeconds(20), TestItem(1))).isTrue

        tracker.track(now.plusSeconds(40), TestItem(4))
        then(tracker.exists(now.plusSeconds(40), TestItem(1))).isFalse
        then(tracker.exists(now.plusSeconds(40), TestItem(2))).isTrue
        then(tracker.exists(now.plusSeconds(40), TestItem(3))).isTrue
    }

    private data class TestItem(val i: Int)
}