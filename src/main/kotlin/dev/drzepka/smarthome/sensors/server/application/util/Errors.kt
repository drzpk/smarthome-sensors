package dev.drzepka.smarthome.sensors.server.application.util

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ObjectError
import dev.drzepka.smarthome.sensors.server.application.ValidationError

fun List<ValidationError>.describeErrors(): List<String> {
    return map { error ->
        when (error) {
            is FieldError -> "Field '${error.field}': ${error.message}"
            is ObjectError -> "Object: ${error.message}"
        }
    }
}
