package dev.drzepka.smarthome.sensors.server.application

import dev.drzepka.smarthome.sensors.server.domain.Page
import dev.drzepka.smarthome.sensors.server.domain.PageQuery
import dev.drzepka.smarthome.sensors.server.domain.TimeRangeQuery
import java.time.Instant

open class PageRequest : PageQuery {
    override var page = 1
    override var size = 10
}

open class TimeRangeRequest : PageRequest(), TimeRangeQuery {
    override var from: Instant? = null
    override var to: Instant? = null

}

@Suppress("unused")
class PagedResourceCollection<T>(val content: Collection<T>, val metadata: PageMetadata) {

    companion object {
        fun <T : Any, U : Any> fromPage(source: Page<T>, mapper: (source: T) -> U): PagedResourceCollection<U> {
            val mapped = source.content.map { mapper.invoke(it) }
            val metadata = PageMetadata(source.page, source.size, source.totalElements, source.totalPages)
            return PagedResourceCollection(mapped, metadata)
        }
    }

    data class PageMetadata(val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
}