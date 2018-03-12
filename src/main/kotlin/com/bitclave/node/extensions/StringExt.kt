package com.bitclave.node.extensions

import org.bitcoinj.core.ECKey
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.Hash
import java.security.spec.ECPoint

fun String.sha3(): ByteArray {
    return Hash.sha3(this.toByteArray())
}

fun String.fromHex(): ByteArray {
    return Hex.decode(this)
}

fun String.ecPoint(): ECPoint {
    return this.fromHex().ecPublicKey().w
}

fun ECPoint.compressedString() : String {
    return ECKey.fromPublicOnly(
            ECKey.CURVE.curve.createPoint(this.affineX, this.affineY).getEncoded(true)
    ).publicKeyAsHex
}

fun ECPoint.uncompressedString() : String {
    return ECKey.fromPublicOnly(
            ECKey.CURVE.curve.createPoint(this.affineX, this.affineY).getEncoded(false)
    ).publicKeyAsHex
}
