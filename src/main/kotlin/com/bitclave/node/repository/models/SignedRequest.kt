package com.bitclave.node.repository.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SignedRequest<T>(
    val data: T? = null,
    val pk: String = "",
    val sig: String = "",
    val nonce: Long = 0,
    val rawData: String = ""
) {
    companion object {
        fun <T> valueOf(gson: Gson, data: String): SignedRequest<T> {
            class Token : TypeToken<SignedRequest<T>>()

            val signRaw: SignedRequest<T> = gson.fromJson(data, Token().type)
            val rawData = gson.toJson(signRaw.data)

            return signRaw.copy(rawData = rawData)
        }
    }

    fun hasSignature(): Boolean = this.sig.isNotBlank()
}
