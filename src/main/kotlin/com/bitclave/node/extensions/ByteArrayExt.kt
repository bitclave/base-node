package com.bitclave.node.extensions

import org.bouncycastle.util.encoders.Hex

fun ByteArray.hex(): String {
    return Hex.toHexString(this)
}
