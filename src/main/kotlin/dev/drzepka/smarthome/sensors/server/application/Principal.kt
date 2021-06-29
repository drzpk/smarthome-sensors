package dev.drzepka.smarthome.sensors.server.application

import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import io.ktor.auth.*

interface SensorsServerPrincipal : Principal

class LoggerPrincipal(val logger: Logger) : SensorsServerPrincipal