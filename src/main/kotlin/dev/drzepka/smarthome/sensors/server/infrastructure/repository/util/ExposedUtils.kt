package dev.drzepka.smarthome.sensors.server.infrastructure.repository.util

import dev.drzepka.smarthome.sensors.server.domain.PageQuery
import dev.drzepka.smarthome.sensors.server.domain.TimeRangeQuery
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*

fun IdTable<*>.countRows(where: Op<Boolean>? = null): Long {
    val countExpression = this.id.count()
    val slice = this.slice(countExpression)
    val countQuery = if (where != null) slice.select(where) else slice.selectAll()
    return countQuery.first()[countExpression]
}

fun Query.pageQuery(pageQuery: PageQuery): Query {
    this.limit(pageQuery.size, (pageQuery.page - 1) * pageQuery.size.toLong())
    return this
}

fun Query.timeRangeQuery(timeRangeQuery: TimeRangeQuery, timeColumn: Column<*>): Query {
    if (timeRangeQuery.from != null)
        andWhere { timeColumn greaterEq timeRangeQuery.from!! }
    if (timeRangeQuery.to != null)
        andWhere { timeColumn less timeRangeQuery.to!! }
    pageQuery(timeRangeQuery)
    return this
}
