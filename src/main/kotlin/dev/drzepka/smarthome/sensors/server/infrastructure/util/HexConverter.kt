package dev.drzepka.smarthome.sensors.server.infrastructure.util

import kotlin.experimental.and

object HexConverter {

    private const val CHARS = "0123456789abcdef"

    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString("") {
            val hi = it.toInt().and(0xF0).ushr(4)
            val lo = it.and(0xF).toInt()
            CHARS[hi].toString() + CHARS[lo].toString()
        }
    }

    fun fromHexString(hex: String): ByteArray {
        if (hex.length.rem(2) != 0)
            throw IllegalArgumentException("Input must be a multiple of 2")

        val array = ByteArray(hex.length / 2)
        hex.forEachIndexed { index, char ->
            var b = CHARS.indexOf(char)
            if (b == -1)
                throw IllegalArgumentException("Illegal character: '$char'")

            b = b.shl((1 - index.rem(2)) * 4)
            array[index.div(2)] = (array[index.div(2)] + b).toByte()
        }

        return array
    }
}

