package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.controllers.EnrichedOffersWithCountersResponse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Type

class PageWithCountersSerializer : JsonSerializer<EnrichedOffersWithCountersResponse> {

    private val tokenForCounters = object : TypeToken<Map<String, Map<String, Int>>>() {}.type

    override fun serialize(
        src: EnrichedOffersWithCountersResponse,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = PageSerializer().serialize(src, typeOfSrc, context).asJsonObject
        json.add("counters",  context.serialize(src.counters, tokenForCounters))
        return json
    }
}
