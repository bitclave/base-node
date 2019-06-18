package com.bitclave.node.extensions

import com.bitclave.node.BaseNodeApplication
import com.bitclave.node.repository.models.SignedRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bitcoinj.core.ECKey
import java.math.BigInteger
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

private val GSON: Gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    .disableHtmlEscaping()
    .create()

fun <T> SignedRequest<T>.signMessage(privateKey: String): SignedRequest<T> {
    val key: ECKey = ECKey.fromPrivate(BigInteger(privateKey, 16))
    val rawData = GSON.toJson(this.data)
    return this.copy(sig = key.signMessage(rawData), rawData = rawData)
}

fun SignedRequest<*>.validateSig(): CompletableFuture<Boolean> {
    return CompletableFuture.supplyAsync(Supplier {
        if (this.sig.isBlank()) {
            return@Supplier false
        }

        val c = ECKey.signedMessageToKey(this.rawData, this.sig)
        c.publicKeyAsHex == this.pk
    }, BaseNodeApplication.FIXED_THREAD_POOL)
}

fun SignedRequest<*>.toJsonString(): String = GSON.toJson(this)
