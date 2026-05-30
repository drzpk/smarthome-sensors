package dev.drzepka.smarthome.sensors.server.application.dto.measurement

data class CreateMeasurementsResponse(
    val created: Int = 0,
    val duplicated: Int = 0,
    val errors: Int = 0,
    val unknown: List<String> = emptyList()
) {
    val total: Int
        get() = created + duplicated + errors
}
