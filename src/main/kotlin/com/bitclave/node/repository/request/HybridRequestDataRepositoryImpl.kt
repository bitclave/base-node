package com.bitclave.node.repository.request

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.fromHex
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.solidity.generated.NameServiceContract
import com.bitclave.node.solidity.generated.RequestDataContract
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.web3j.tuples.generated.Tuple8
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPublicKeySpec
import java.security.spec.InvalidKeySpecException

@Component
@Qualifier("hybrid")
class HybridRequestDataRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val hybridProperties: HybridProperties
) : RequestDataRepository {

    private val nameServiceData = hybridProperties.contracts.nameService
    private lateinit var nameServiceContract: NameServiceContract
    private lateinit var contract: RequestDataContract

    init {
        nameServiceContract = NameServiceContract.load(
                nameServiceData.address,
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )

        contract = RequestDataContract.load(
                nameServiceContract.addressOfName("requestData").send(),
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )
    }

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

    private fun tupleToRequestData(tuple: Tuple8<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, ByteArray, ByteArray, BigInteger>): RequestData {
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
        val spec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val kf = KeyFactory.getInstance("ECDSA", BouncyCastleProvider())
        val params = ECNamedCurveSpec("secp256k1", spec.curve, spec.g, spec.n)
        val point = ECPointUtil.decodePoint(params.curve, pubKey)
        val pubKeySpec = ECPublicKeySpec(point, params)
        return kf.generatePublic(pubKeySpec) as ECPublicKey
    }
}