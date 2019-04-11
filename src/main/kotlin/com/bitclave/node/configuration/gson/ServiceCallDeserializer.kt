package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.services.HttpServiceCall
import com.bitclave.node.repository.models.services.ServiceCall
import com.bitclave.node.repository.models.services.ServiceCallType
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ServiceCallDeserializer : JsonDeserializer<ServiceCall> {

    private val httpServiceCallToken = object : TypeToken<HttpServiceCall>() {}.type

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ServiceCall {
        if (json.isJsonObject && json.asJsonObject.has("serviceId") && json.asJsonObject.has("type")) {
            val type = ServiceCallType.valueOf(json.asJsonObject["type"].asString)

            if (ServiceCallType.HTTP == type) {
                return context.deserialize<HttpServiceCall>(json, httpServiceCallToken)
            }
        }

        throw RuntimeException("Wrong Service call object")
    }
}
