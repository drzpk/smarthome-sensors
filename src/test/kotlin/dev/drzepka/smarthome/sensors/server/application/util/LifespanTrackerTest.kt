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
    }

    @Test
    fun `should track lifespan of multiple objects independently`() {
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

    @Test
    fun `should not replace tracked time with older time`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        tracker.track(now.plusSeconds(20), TestItem(1))
        tracker.track(now, TestItem(1))

        then(tracker.exists(now.plusSeconds(45), TestItem(1))).isTrue
        then(tracker.exists(now.plusSeconds(51), TestItem(1))).isFalse
    }

    @Test
    fun `isTracked should return false before tracking and true after`() {
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        then(tracker.isTracked(TestItem(1))).isFalse

        tracker.track(Instant.now(), TestItem(1))

        then(tracker.isTracked(TestItem(1))).isTrue
        then(tracker.isTracked(TestItem(2))).isFalse
    }

    @Test
    fun `existsOrTrack should return false and track on first call`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        then(tracker.existsOrTrack(now, TestItem(1))).isFalse

        then(tracker.exists(now.plusSeconds(15), TestItem(1))).isTrue
    }

    @Test
    fun `existsOrTrack should return true when within lifespan`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        tracker.track(now, TestItem(1))

        then(tracker.existsOrTrack(now.plusSeconds(15), TestItem(1))).isTrue
        then(tracker.existsOrTrack(now.plusSeconds(31), TestItem(1))).isFalse
    }

    @Test
    fun `existsOrTrack should advance tracked time when not a duplicate`() {
        val now = Instant.now()
        val tracker = LifespanTracker<TestItem>(Duration.ofSeconds(30L))

        tracker.existsOrTrack(now, TestItem(1))
        tracker.existsOrTrack(now.plusSeconds(31), TestItem(1))

        then(tracker.exists(now.plusSeconds(55), TestItem(1))).isTrue
        then(tracker.exists(now.plusSeconds(62), TestItem(1))).isFalse
    }

    private data class TestItem(val i: Int)
}
