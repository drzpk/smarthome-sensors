package dev.drzepka.smarthome.sensors.server.domain.util

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConversionsTest {

    @Test
    fun `asInt should return correct value for different number types`() {
        then(42.asInt()).isEqualTo(42)
        then(42L.asInt()).isEqualTo(42)
        then(1.4f.asInt()).isEqualTo(1)
        then(1.5f.asInt()).isEqualTo(2)
        then(1.4.asInt()).isEqualTo(1)
        then(1.5.asInt()).isEqualTo(2)
        then(BigDecimal("1.4").asInt()).isEqualTo(1)
        then(BigDecimal("1.5").asInt()).isEqualTo(2)
    }

    @Test
    fun `asBigDecimal should return correct value for different number types`() {
        then(BigDecimal("3.14").asBigDecimal()).isEqualTo(BigDecimal("3.14"))
        then(3.14f.asBigDecimal()).isEqualTo(3.14f.toBigDecimal())
        then(3.14.asBigDecimal()).isEqualTo(3.14.toBigDecimal())
        then(42.asBigDecimal()).isEqualTo(BigDecimal.valueOf(42))
        then(42L.asBigDecimal()).isEqualTo(BigDecimal.valueOf(42))
    }

    @Test
    fun `asFloat should return correct value for different number types`() {
        then(3.14f.asFloat()).isEqualTo(3.14f)
        then(42.asFloat()).isEqualTo(42.0f)
        then(42L.asFloat()).isEqualTo(42.0f)
        then(3.14.asFloat()).isEqualTo(3.14f)
        then(BigDecimal("3.14").asFloat()).isEqualTo(3.14f)
    }
}
