package com.bitclave.node.extensions

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.util.encoders.Hex
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPublicKeySpec
import java.security.spec.InvalidKeySpecException

fun ByteArray.hex(): String = Hex.toHexString(this)

// https://stackoverflow.com/a/26159150/440168
@Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
fun ByteArray.ecPublicKey(): ECPublicKey {
    val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
    val kf = KeyFactory.getInstance("ECDSA", BouncyCastleProvider())
    val params = ECNamedCurveSpec("secp256k1", spec.curve, spec.g, spec.n)
    val point = ECPointUtil.decodePoint(params.curve, this)
    val pubKeySpec = ECPublicKeySpec(point, params)

    return kf.generatePublic(pubKeySpec) as ECPublicKey
}