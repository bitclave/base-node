package com.bitclave.node.extensions

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.stream.Collectors

private val EXISTED_CAMEL_CASE_ITEMS = mutableMapOf<String, String>()
private val EXISTED_SNAKE_CASE_ITEMS = mutableMapOf<String, String>()

fun Pageable.changeOrderFieldsToSnakeCase(): Pageable = convertOrders(this, true)

fun Pageable.changeOrderFieldsToCamelCase(): Pageable = convertOrders(this, false)

private fun convertOrders(pageable: Pageable, fromCamelToSnake: Boolean): Pageable {
    val orders = pageable.sort.get().map {
        val prop = if (fromCamelToSnake) convertToSnakeCase(it.property) else convertSnakeCaseToCamelCase(it.property)
        if (it.direction == Sort.Direction.ASC)
            Sort.Order.asc(prop)
        else
            Sort.Order.desc(prop)

    }.collect(Collectors.toList())

    return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
}

private fun convertSnakeCaseToCamelCase(value: String): String {
    if (EXISTED_CAMEL_CASE_ITEMS.containsKey(value)) {
        return EXISTED_CAMEL_CASE_ITEMS[value]!!
    }

    val result = value.replace(Regex("_\\w")) { it.value.subSequence(1, 2).toString().toUpperCase() }

    EXISTED_CAMEL_CASE_ITEMS[value] = result

    return result
}

private fun convertToSnakeCase(value: String): String {
    if (EXISTED_SNAKE_CASE_ITEMS.containsKey(value)) {
        return EXISTED_SNAKE_CASE_ITEMS[value]!!
    }

    val result = value.replace(Regex("[A-Z]")) { "_${it.value.toLowerCase()}" }

    EXISTED_SNAKE_CASE_ITEMS[value] = result

    return result
}
