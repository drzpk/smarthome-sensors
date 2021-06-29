package dev.drzepka.smarthome.sensors.server.domain

import java.time.Instant
import kotlin.math.ceil

interface PageQuery {
    var page: Int
    var size: Int
}

interface TimeRangeQuery : PageQuery {
    var from: Instant?
    var to: Instant?
}

data class Page<T : Any>(
    val content: Collection<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long
) {
    constructor(content: Collection<T>, pageQuery: PageQuery, totalElements: Long) : this(
        content,
        pageQuery.page,
        pageQuery.size,
        totalElements
    )

    val totalPages: Int
        get() = ceil(totalElements.toDouble() / size).toInt()
}