package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.controllers.OffersWithCountersResponse
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Type
import com.google.gson.Gson

class PageWithCountersResponseDeserializer : PageResponseDeserializer() {
    private val pageTokenType = object : TypeToken<PageImpl<Long>>() {}.type
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): OffersWithCountersResponse {
        val one = super.deserialize(json, pageTokenType, context) as Page<Long>
        val response = json!!.asJsonObject
        val empMapType = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
        val rawCounters = response.getAsJsonObject("counters")
        val counters: Map<String, Map<String, Int>> = Gson().fromJson(rawCounters, empMapType)
        return OffersWithCountersResponse(counters, one)
    }
}
