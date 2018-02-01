package com.bitclave.node.utils

import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.util.encoders.Hex

class Sha3Utils {

    companion object {

        fun stringToSha3Hex(message: String): String {
            val digestSHA3 = SHA3.Digest256()
            val digest = digestSHA3.digest(message.toByteArray())

            return Hex.toHexString(digest)
        }

    }

}
