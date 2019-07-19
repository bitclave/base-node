package com.bitclave.node.configuration.gson

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import java.lang.reflect.Type

class SliceSerializer : JsonSerializer<Slice<*>> {

    override fun serialize(
        src: Slice<*>,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = context.serialize(src, SliceImpl::class.java).asJsonObject
        json.addProperty("numberOfElements", src.numberOfElements)
        json.addProperty("first", src.isFirst)
        json.addProperty("hasNext", src.hasNext())
        json.addProperty("last", src.isLast)
        json.addProperty("number", src.number)
        json.addProperty("size", src.size)

        return json
    }
}
