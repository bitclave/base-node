package com.bitclave.node.configuration.gson

import com.bitclave.node.models.SignedRequest
import com.bitclave.node.services.errors.BadArgumentException
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import mu.KotlinLogging
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class SignedRequestDeserializer : JsonDeserializer<SignedRequest<*>> {

    val logger = KotlinLogging.logger { }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): SignedRequest<*> {
        try {
            val parameterizedType = typeOfT as ParameterizedType

            if (json != null && parameterizedType.actualTypeArguments.size >= 0) {
                val dataType = parameterizedType.actualTypeArguments[0]
                val signedRequest = json.asJsonObject

                val pk = signedRequest["pk"].asString
                val sig = signedRequest["sig"].asString
                val nonce = signedRequest["nonce"].asLong

                val rawData: String = when {
                    signedRequest["data"].isJsonObject ->
                        signedRequest["data"].asJsonObject.toString()

                    signedRequest["data"].isJsonArray ->
                        signedRequest["data"].asJsonArray.toString()

                    signedRequest["data"].isJsonNull -> ""

                    else -> signedRequest["data"].toString()
                }

                val jsonElementData = signedRequest["data"]

                val data: Any = context.deserialize(jsonElementData, dataType)

                return SignedRequest(data, pk, sig, nonce, rawData)
            }
        } catch (e: Throwable) {
            logger.error("Signed Request: $e")
        }

        throw BadArgumentException("Signed request wrong data!")
    }
}
