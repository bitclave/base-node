package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.controllers.EnrichedOffersWithCountersResponse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class PageWithCountersSerializer : JsonSerializer<EnrichedOffersWithCountersResponse> {

    override fun serialize(
        src: EnrichedOffersWithCountersResponse,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val json = PageSerializer().serialize(src, typeOfSrc, context).asJsonObject
        val counters = JsonObject()
        src.counters.forEach { (key, value) ->
            val aggregation = JsonObject()
            value.forEach { (name, counter) ->
                aggregation.addProperty(name, counter)
            }
            counters.add(key, aggregation)
        }
        json.add("counters", counters)
        return json
    }
}
