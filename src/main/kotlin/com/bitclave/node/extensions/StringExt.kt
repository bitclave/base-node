package com.bitclave.node.extensions

import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.Hash

fun String.sha3(): ByteArray {
    return Hash.sha3(this.toByteArray())
}

fun String.fromHex(): ByteArray {
    return Hex.decode(this)
}
