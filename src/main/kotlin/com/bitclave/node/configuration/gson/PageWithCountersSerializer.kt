package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.controllers.EnrichedOffersWithCountersResponse
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import org.springframework.data.domain.Page
import java.lang.reflect.Type

class PageWithCountersSerializer : JsonSerializer<EnrichedOffersWithCountersResponse> {

    private val pageTokenType = object : TypeToken<Page<Long>>() {}.type
    private val tokenForCounters = object : TypeToken<Map<String, Map<String, Int>>>() {}.type

    override fun serialize(
        src: EnrichedOffersWithCountersResponse,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = context.serialize(src, pageTokenType).asJsonObject
        val counters = context.serialize(src.counters, tokenForCounters)
        json.add("counters", counters)

        return json
    }
}
