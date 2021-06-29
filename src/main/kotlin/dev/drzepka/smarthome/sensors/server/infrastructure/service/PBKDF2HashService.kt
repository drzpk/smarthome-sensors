package dev.drzepka.smarthome.sensors.server.infrastructure.service

import dev.drzepka.smarthome.sensors.server.application.service.HashService
import dev.drzepka.smarthome.sensors.server.infrastructure.util.HexConverter
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

class PBKDF2HashService : HashService {

    private val secureRandom = SecureRandom()
    private val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    override fun createHash(password: String): String {
        val salt = createSalt()
        return createHash(password, salt, ITERATIONS, HASH_BYTES)
    }

    override fun compareHashes(existingHash: String, password: String): Boolean {
        val parts = existingHash.split(":")
        if (parts.size != 3)
            throw IllegalArgumentException("Invalid existing hash format")

        val iterations = parts[0].toInt()
        val salt = HexConverter.fromHexString(parts[1])
        val hashBytes = HexConverter.fromHexString(parts[2]).size

        val newHash = createHash(password, salt, iterations, hashBytes)
        return compareInConstantTime(existingHash, newHash)
    }

    private fun createSalt(): ByteArray {
        val array = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(array)
        return array
    }

    private fun createHash(password: String, salt: ByteArray, iterations: Int, hashBytes: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, hashBytes * 8)
        val hexHash = HexConverter.toHexString(factory.generateSecret(spec).encoded)
        val hexSalt = HexConverter.toHexString(salt)

        return "$iterations:$hexSalt:$hexHash"
    }

    private fun compareInConstantTime(left: String, right: String): Boolean {
        var eq = left.length == right.length
        val min = min(left.length, right.length)

        for (i in 0 until min) {
            eq = eq.and(left[i] == right[i])
        }

        return eq
    }

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val HASH_BYTES = 32
        private const val SALT_BYTES = 16
        private const val ITERATIONS = 1000
    }
}