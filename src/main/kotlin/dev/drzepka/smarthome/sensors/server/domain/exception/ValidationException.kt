package dev.drzepka.smarthome.sensors.server.domain.exception

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors


class ValidationException(val validationErrors: ValidationErrors) : RuntimeException("Validation error")