package com.bitclave.node.extensions

import com.bitclave.node.repository.models.SignedRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bitcoinj.core.ECKey
import java.math.BigInteger
import java.util.concurrent.CompletableFuture

private val GSON: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .disableHtmlEscaping()
        .create()

fun SignedRequest<*>.signMessage(privateKey: String) {
    val key: ECKey = ECKey.fromPrivate(BigInteger(privateKey, 16))
    this.sig = key.signMessage(GSON.toJson(this.data))
}

fun SignedRequest<*>.validateSig(): CompletableFuture<Boolean> {
    return CompletableFuture.supplyAsync {
        val c = ECKey.signedMessageToKey(this.rawData, this.sig)
        c.publicKeyAsHex == this.pk
    }
}

fun SignedRequest<*>.toJsonString(): String = GSON.toJson(this)
