package dev.drzepka.smarthome.sensors.server.domain.exception

import java.lang.RuntimeException

class NotFoundException(message: String) : RuntimeException(message)