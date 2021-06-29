package dev.drzepka.smarthome.sensors.server.application.service

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.regex.Pattern

@ExtendWith(MockitoExtension::class)
class PasswordGeneratorServiceTest {

    private val hashService = mock<HashService>()

    @Test
    fun `should generate password without digits`() {
        whenever(hashService.createHash(any())).thenReturn("hash")

        val pattern = Pattern.compile("[a-zA-Z]+")
        val service = getService()

        repeat(100) {
            val generated = service.generatePassword(10, 20, false)
            then(generated.hash).isEqualTo("hash")
            then(generated.plainText)
                .hasSizeBetween(10, 20)
                .matches(pattern)
        }
    }

    @Test
    fun `should generate password with digits`() {
        whenever(hashService.createHash(any())).thenReturn("hash")

        val pattern = Pattern.compile("[a-zA-Z0-9]+")
        val service = getService()

        repeat(100) {
            val generated = service.generatePassword(10, 20, true)
            then(generated.hash).isEqualTo("hash")
            then(generated.plainText)
                .hasSizeBetween(10, 20)
                .matches(pattern)
                .matches { password ->
                    password.findAnyOf(IntRange(0, 9).map { it.toString() }) != null
                }
        }
    }

    private fun getService(): PasswordGeneratorService = PasswordGeneratorService(hashService)

}