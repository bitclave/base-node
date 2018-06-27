package com.bitclave.node.extensions

import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.InvalidKeySpecException

fun ECPoint(str: String): ECPoint {
    return ECPublicKey(str.fromHex()).w
}

fun ECPoint.compressedString(): String {
    return ECKey.fromPublicOnly(
            ECKey.CURVE.curve.createPoint(this.affineX, this.affineY).getEncoded(true)
    ).publicKeyAsHex
}

fun ECPoint.uncompressedString(): String {
    return ECKey.fromPublicOnly(
            ECKey.CURVE.curve.createPoint(this.affineX, this.affineY).getEncoded(false)
    ).publicKeyAsHex
}

// https://stackoverflow.com/a/26159150/440168
@Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
fun ECPublicKey(arr: ByteArray): ECPublicKey {
    val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val kf = KeyFactory.getInstance("ECDSA", BouncyCastleProvider())
    val params = ECNamedCurveSpec("secp256k1", spec.curve, spec.g, spec.n)
    val point = ECPointUtil.decodePoint(params.curve, arr)
    val pubKeySpec = ECPublicKeySpec(point, params)

    return kf.generatePublic(pubKeySpec) as ECPublicKey
}
