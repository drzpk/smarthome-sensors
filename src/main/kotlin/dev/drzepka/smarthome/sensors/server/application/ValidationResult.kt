package dev.drzepka.smarthome.sensors.server.application

import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()

    fun throwIfInvalid() {
        if (this is Invalid) {
            val ve = ValidationErrors()
            errors.forEach {
                when (it) {
                    is FieldError -> ve.addFieldError(it.field, it.message)
                    is ObjectError -> ve.addObjectError(it.message)
                }
            }
            throw ValidationException(ve)
        }
    }
}
