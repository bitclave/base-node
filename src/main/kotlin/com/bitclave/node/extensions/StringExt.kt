package com.bitclave.node.extensions

import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.util.encoders.Hex

fun String.sha3(): ByteArray {
    val digestSHA3 = SHA3.Digest256()
    return digestSHA3.digest(this.toByteArray())
}

fun String.fromHex(): ByteArray {
    return Hex.decode(this)
}
