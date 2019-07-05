package com.bitclave.node.configuration.gson

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Type

class PageSerializer : JsonSerializer<Page<*>> {

    override fun serialize(
        src: Page<*>,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val totalElements = if (src.totalElements < 10000) src.totalElements.toInt() else 10000
        val totalPages = if (src.size != 0) totalElements / src.size else 0

        val json = context.serialize(src, PageImpl::class.java).asJsonObject
        json.addProperty("numberOfElements", src.numberOfElements)
        json.addProperty("first", src.isFirst)
        json.addProperty("last", src.isLast)
        json.addProperty("number", src.number)
        json.addProperty("size", src.size)
        json.addProperty("totalPages", totalPages)
        json.addProperty("totalElements", totalElements)

        return json
    }
}
