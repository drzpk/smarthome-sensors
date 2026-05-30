package dev.drzepka.smarthome.sensors.server.application.dto.measurement

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal
import java.time.Instant

class CreateMeasurementsRequest {
    var measurements = ArrayList<Measurement>()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TemperatureMeasurement::class, name = "TEMPERATURE"),
    JsonSubTypes.Type(value = PvMeasurement::class, name = "PV"),
)
interface Measurement {
    val mac: String
    val time: Instant?
}

class TemperatureMeasurement(
    override val mac: String,
    override val time: Instant?,
    val temperature: BigDecimal = BigDecimal.ZERO,
    val humidity: BigDecimal = BigDecimal.ZERO,
    val batteryVoltage: BigDecimal? = null,
    val batteryLevel: Int? = null
) : Measurement

data class PvMeasurement(
    override val mac: String,
    override val time: Instant?,
    val totalPower: Int,
    val energyToday: BigDecimal,
    val energyTotal: BigDecimal,
    val phaseA: Phase?,
    val phaseB: Phase?,
    val phaseC: Phase?,
    val pv1: Pv?,
    val pv2: Pv?
) : Measurement

data class Phase(
    val voltage: Float,
    val current: Float,
    val power: Int?,
    val frequency: Float
)

data class Pv(
    val voltage: Float,
    val current: Float,
    val power: Int,
    val energyToday: BigDecimal
)
