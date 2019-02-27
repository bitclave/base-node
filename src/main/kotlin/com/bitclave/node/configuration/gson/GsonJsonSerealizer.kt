package com.bitclave.node.configuration.gson

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import springfox.documentation.spring.web.json.Json
import java.lang.reflect.Type

class SpringfoxJsonSerializer : JsonSerializer<Json> {

    override fun serialize(json: Json, type: Type, context: JsonSerializationContext): JsonElement {
        val parser = JsonParser()
        return parser.parse(json.value())
    }
}
