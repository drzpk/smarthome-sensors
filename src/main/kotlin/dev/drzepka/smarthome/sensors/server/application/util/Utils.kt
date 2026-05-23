package dev.drzepka.smarthome.sensors.server.application.util

@Suppress("UNCHECKED_CAST")
fun <K, V> mapOfNotNull(vararg pairs: Pair<K?, V?>): Map<K, V> =
    mapOf(*pairs)
        .filter { it.key != null && it.value != null } as Map<K, V>
