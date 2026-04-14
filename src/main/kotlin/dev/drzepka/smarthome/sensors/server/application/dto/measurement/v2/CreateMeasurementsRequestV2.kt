package dev.drzepka.smarthome.sensors.server.application.dto.measurement.v2

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.math.BigDecimal

class CreateMeasurementsRequestV2 {
    var measurements = ArrayList<Measurement>()

    class Measurement {
        var deviceId = 0
        var timestampOffsetMillis: Long = 0
        lateinit var data: MeasurementData

        override fun toString(): String =
            "Measurement(deviceId=$deviceId, timestampOffsetMillis=$timestampOffsetMillis, data=$data)"
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TemperatureData::class, name = "temperature")
)
sealed class MeasurementData

data class TemperatureData(
    val temperature: BigDecimal = BigDecimal.ZERO,
    val humidity: BigDecimal = BigDecimal.ZERO,
    val batteryVoltage: BigDecimal? = null,
    val batteryLevel: Int? = null
) : MeasurementData()
