package dev.drzepka.smarthome.sensors.server.application

import dev.drzepka.smarthome.sensors.server.domain.entity.Logger

interface SensorsServerPrincipal

class LoggerPrincipal(val logger: Logger) : SensorsServerPrincipal