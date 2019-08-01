package com.bitclave.node.configuration.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

open class PageResponseDeserializer : JsonDeserializer<Page<*>> {

    val pageableType = object : TypeToken<PageRequest>() {}.type

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Page<*> {
        val parameterizedType = typeOfT as ParameterizedType
        val dataType = parameterizedType.actualTypeArguments[0]
        val response = json!!.asJsonObject

        var pageable: PageRequest? = null
        var content: List<Any>? = null
        var total: Long = 0

        if (response.has("pageable") && response.get("pageable").isJsonObject) {
            pageable = context!!.deserialize<PageRequest>(response.getAsJsonObject("pageable"), pageableType)
        }

        if (response.has("content") && response.get("content").isJsonArray) {
            val array = response.getAsJsonArray("content")
            content = arrayListOf()
            for (item in array) {
                content.add(context!!.deserialize(item, dataType))
            }
        }

        if (response.has("total") && response.get("total").isJsonPrimitive) {
            total = response.getAsJsonPrimitive("total").asLong
        }

        return if (content != null && pageable != null) {
            PageImpl(content, pageable, total)
        } else if (content != null) {
            PageImpl(content)
        } else {
            PageImpl(emptyList())
        }
    }
}
