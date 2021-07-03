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

        then(tracker.exists(TestItem(999))).isFalse

        tracker.track(now, TestItem(11))
        then(tracker.exists(TestItem(11), now.plusSeconds(15))).isTrue
        then(tracker.exists(TestItem(11), now.plusSeconds(29))).isTrue
        then(tracker.exists(TestItem(11), now.plusSeconds(30))).isTrue
        then(tracker.exists(TestItem(11), now.plusSeconds(31))).isFalse

        then(tracker.track(now.plusSeconds(31), TestItem(98), now = now.plusSeconds(31)))
        then(tracker.exists(TestItem(11), now.plusSeconds(15))).isFalse
    }

    @Test
    fun `should track lifespan of multiple objects`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        tracker.track(now, TestItem(1))
        tracker.track(now.plusSeconds(10), TestItem(2))
        tracker.track(now.plusSeconds(20), TestItem(3))

        then(tracker.exists(TestItem(1), now.plusSeconds(20))).isTrue

        tracker.track(now.plusSeconds(40), TestItem(4))
        then(tracker.exists(TestItem(1), now.plusSeconds(40))).isFalse
        then(tracker.exists(TestItem(2), now.plusSeconds(40))).isTrue
        then(tracker.exists(TestItem(3), now.plusSeconds(40))).isTrue
    }

    private data class TestItem(val i: Int)
}