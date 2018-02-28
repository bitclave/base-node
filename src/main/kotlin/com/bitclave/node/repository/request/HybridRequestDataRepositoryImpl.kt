package com.bitclave.node.repository.request

import com.bitclave.node.extensions.fromHex
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.solidity.generated.RequestDataContract
import org.bouncycastle.jce.ECNamedCurveTable
import org.web3j.tuples.generated.Tuple8
import java.math.BigInteger
import java.nio.charset.Charset
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.spec.InvalidKeySpecException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPublicKeySpec


class HybridRequestDataRepositoryImpl(val contract: RequestDataContract) :
        RequestDataRepository {

    override fun getByFrom(from: String, state: RequestData.RequestDataState): List<RequestData> {
        val count = contract.getByFromCount(publicKeyX(from), state.ordinal.toBigInteger()).send().toLong()
        return (0..(count - 1))
                .map {
                    contract.getByFrom(publicKeyX(from), state.ordinal.toBigInteger(), it.toBigInteger()).send()
                }
                .map {
                    contract.findById(it).send()
                }
                .map {
                    tupleToRequestData(it)
                }
                .toList()
    }

    override fun getByTo(to: String, state: RequestData.RequestDataState): List<RequestData> {
        val count = contract.getByToCount(publicKeyX(to), state.ordinal.toBigInteger()).send().toLong()
        return (0..(count - 1))
                .map {
                    contract.getByFrom(publicKeyX(to), state.ordinal.toBigInteger(), it.toBigInteger()).send()
                }
                .map {
                    contract.findById(it).send()
                }
                .map {
                    tupleToRequestData(it)
                }
                .toList()
    }

    override fun getByFromAndTo(
            from: String,
            to: String,
            state: RequestData.RequestDataState
    ): List<RequestData> {

        val count = contract.getByFromAndToCount(publicKeyX(from), publicKeyX(to), state.ordinal.toBigInteger()).send().toLong()
        return (0..(count - 1))
                .map {
                    contract.getByFromAndTo(publicKeyX(from), publicKeyX(to), state.ordinal.toBigInteger(), it.toBigInteger()).send()
                }
                .map {
                    contract.findById(it).send()
                }
                .map {
                    tupleToRequestData(it)
                }
                .toList()
    }

    override fun findById(id: Long): RequestData? {
        return tupleToRequestData(contract.findById(id.toBigInteger()).send())
    }

    override fun updateData(request: RequestData): RequestData {
        contract.updateData(
                request.id.toBigInteger(),
                publicKeyX(request.fromPk),
                publicKeyY(request.fromPk),
                publicKeyX(request.toPk),
                publicKeyY(request.toPk),
                request.requestData.toByteArray(),
                request.responseData.toByteArray(),
                request.state.ordinal.toBigInteger())
        return request
    }

    // Private

    private fun tupleToRequestData(tuple: Tuple8<BigInteger,BigInteger,BigInteger,BigInteger,BigInteger,ByteArray,ByteArray,BigInteger>): RequestData {
        return RequestData(
                tuple.value1.toLong(),
                "04"
                        + tuple.value2.toString(16).padStart(32, '0')
                        + tuple.value3.toString(16).padStart(32, '0'),
                "04"
                        + tuple.value4.toString(16).padStart(32, '0')
                        + tuple.value5.toString(16).padStart(32, '0'),
                tuple.value6.toString(Charset.defaultCharset())
                        .trim(Character.MIN_VALUE),
                tuple.value7.toString(Charset.defaultCharset())
                        .trim(Character.MIN_VALUE),
                RequestData.RequestDataState.values()[tuple.value8.toInt()])
    }

    private fun publicKeyX(publicKey: String): BigInteger {
        return BigInteger(publicKey.substring(2, 64), 16)
    }

    private fun publicKeyY(publicKey: String): BigInteger {
        if (publicKey.length == 130) {
            return BigInteger(publicKey.substring(66, 64), 16)
        }
        return getPublicKeyFromBytes(publicKey.fromHex()).w.affineY
    }

    // https://stackoverflow.com/a/26159150/440168
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getPublicKeyFromBytes(pubKey: ByteArray): ECPublicKey {
        val spec = ECNamedCurveTable.getParameterSpec("prime256v1")
        val kf = KeyFactory.getInstance("ECDSA", BouncyCastleProvider())
        val params = ECNamedCurveSpec("prime256v1", spec.curve, spec.g, spec.n)
        val point = ECPointUtil.decodePoint(params.curve, pubKey)
        val pubKeySpec = ECPublicKeySpec(point, params)
        return kf.generatePublic(pubKeySpec) as ECPublicKey
    }
}