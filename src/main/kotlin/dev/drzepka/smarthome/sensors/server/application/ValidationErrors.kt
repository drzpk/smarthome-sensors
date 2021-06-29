package dev.drzepka.smarthome.sensors.server.application

import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException

class ValidationErrors {

    val errors = ArrayList<ValidationError>()

    fun addFieldError(field: String, message: String) {
        errors.add(FieldError(field, message))
    }

    fun addObjectError(message: String) {
        errors.add(ObjectError(message))
    }

    fun verify() {
        if (errors.isNotEmpty())
            throw ValidationException(this)
    }
}

sealed class ValidationError(val message: String)

class ObjectError(message: String) : ValidationError(message)

class FieldError(val field: String, message: String) : ValidationError(message)