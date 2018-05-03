package com.bitclave.node.utils

import org.bitcoinj.core.ECKey
import java.math.BigInteger

class KeyPairUtils {

    companion object {

        fun isValidPublicKey(publicKey: String): Boolean {
            return try {
                val pk =BigInteger(publicKey, 16)
                val point = ECKey.CURVE.curve.decodePoint(pk.toByteArray())

                point.isValid
            } catch (e: Exception) {
                false
            }
        }

    }
}