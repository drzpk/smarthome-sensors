package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.GeneratedPassword
import dev.drzepka.smarthome.sensors.server.application.dto.logger.CreateLoggerRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Logger
import dev.drzepka.smarthome.sensors.server.domain.repository.LoggerRepository
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class LoggerServiceTest {

    private val loggerRepository = mock<LoggerRepository>()
    private val passwordGeneratorService = mock<PasswordGeneratorService>()
    private val hashService = mock<HashService>()

    @Test
    fun `should create logger`() {
        val request = CreateLoggerRequest().apply {
            name = "name"
            description = "description"
        }
        val generatedPassword = GeneratedPassword("plain", "hash")

        whenever(passwordGeneratorService.generatePassword(any(), any(), any())).thenReturn(generatedPassword)
        whenever(loggerRepository.save(any())).thenAnswer { it.getArgument(0, Logger::class.java).id = 123; 0  }

        val resource = getService().createLogger(request)

        then(resource.name).isEqualTo(request.name)
        then(resource.description).isEqualTo(request.description)
        then(resource.password).isEqualTo(generatedPassword.plainText)

        val captor = argumentCaptor<Logger>()
        verify(loggerRepository, times(1)).save(captor.capture())

        val entity = captor.firstValue
        then(entity.name).isEqualTo(request.name)
        then(entity.description).isEqualTo(request.description)
        then(entity.active).isTrue()
        then(entity.password).isEqualTo(generatedPassword.hash)
    }

    @Test
    fun `should delete logger`() {
        val entity = Logger().apply {
            id = 123
            active = true
        }

        whenever(loggerRepository.findById(eq(123))).thenReturn(entity)

        getService().deleteLogger(123)

        then(entity.active).isFalse()
        verify(loggerRepository).save(same(entity))
    }

    @Test
    fun `should reset logger password`() {
        val entity = Logger().apply {
            id = 123
            active = true
        }
        val generatedpassword = GeneratedPassword("plain", "hash")

        whenever(loggerRepository.findById(eq(123))).thenReturn(entity)
        whenever(passwordGeneratorService.generatePassword(any(), any(), any())).thenReturn(generatedpassword)

        val resource = getService().resetPassword(123)

        then(resource.password).isEqualTo("plain")
        then(entity.password).isEqualTo("hash")
    }

    private fun getService(): LoggerService = LoggerService(loggerRepository, passwordGeneratorService, hashService)
}