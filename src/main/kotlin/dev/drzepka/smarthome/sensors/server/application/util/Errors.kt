package dev.drzepka.smarthome.sensors.server.application.util

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.ObjectError
import dev.drzepka.smarthome.sensors.server.application.ValidationErrors

fun ValidationErrors.describeErrors(): List<String> {
    val text = ArrayList<String>(this.errors.size)

    for (error in this.errors) {
        val msg = when (error) {
            is FieldError -> "Field '${error.field}': ${error.message}"
            is ObjectError -> "Object: ${error.message}"
        }
        text.add(msg)
    }

    return text
}