package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.controllers.OffersWithCountersResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Type

class PageWithCountersResponseDeserializer : JsonDeserializer<OffersWithCountersResponse> {

    private val pageTokenType = object : TypeToken<PageImpl<Long>>() {}.type
    private val mapTokenType = object : TypeToken<Map<String, Map<String, Int>>>() {}.type

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): OffersWithCountersResponse {
        val page = context.deserialize<Page<Long>>(json, pageTokenType)
        val response = json.asJsonObject
        val rawCounters = response.getAsJsonObject("counters")
        val counters = context.deserialize<Map<String, Map<String, Int>>>(rawCounters, mapTokenType)

        return OffersWithCountersResponse(counters, page)
    }
}
