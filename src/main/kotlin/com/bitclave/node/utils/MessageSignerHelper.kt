package com.bitclave.node.utils

import org.bitcoinj.core.ECKey
import java.util.concurrent.CompletableFuture

class MessageSignerHelper {

    companion object {

        fun getMessageSignature(message: String, sig: String): CompletableFuture<String> {
            return CompletableFuture.supplyAsync({ ECKey.signedMessageToKey(message, sig).publicKeyAsHex })
        }

    }

}

