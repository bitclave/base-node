package com.bitclave.node.configuration.gson

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Type

class PageSerializer : JsonSerializer<Page<*>> {

    override fun serialize(src: Page<*>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return context!!.serialize(src, PageImpl::class.java)
    }
}
