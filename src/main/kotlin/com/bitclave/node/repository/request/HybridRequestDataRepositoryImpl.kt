package com.bitclave.node.repository.request

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.*
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.services.errors.DataNotSaved
import com.bitclave.node.solidity.generated.NameServiceContract
import com.bitclave.node.solidity.generated.RequestDataContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.web3j.tuples.generated.Tuple8
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.spec.ECPoint

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
        val ecPointFrom = ECPoint(from)

        val count = contract.getByFromCount(ecPointFrom.affineX, state.ordinal.toBigInteger()).send().toLong()
        return (0..(count - 1))
                .map {
                    contract.getByFrom(
                            ecPointFrom.affineX,
                            state.ordinal.toBigInteger(),
                            it.toBigInteger()
                    ).send()
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
        val ecPointTo = ECPoint(to)

        val count = contract.getByToCount(
                ecPointTo.affineX,
                state.ordinal.toBigInteger()
        )
                .send()
                .toLong()
        return (0..(count - 1))
                .map {
                    contract.getByTo(
                            ecPointTo.affineX,
                            state.ordinal.toBigInteger(),
                            it.toBigInteger()
                    ).send()
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

        val ecPointFrom = ECPoint(from)
        val ecPointTo = ECPoint(to)

        val count = contract.getByFromAndToCount(
                ecPointFrom.affineX,
                ecPointTo.affineX,
                state.ordinal.toBigInteger()
        )
                .send()
                .toLong()
        return (0..(count - 1))
                .map {
                    contract.getByFromAndTo(
                            ecPointFrom.affineX,
                            ecPointTo.affineX,
                            state.ordinal.toBigInteger(),
                            it.toBigInteger()
                    ).send()
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
        val ecPointFrom = ECPoint(request.fromPk)
        val ecPointTo = ECPoint(request.toPk)

        try {
            val tx = contract.updateData(
                    request.id.toBigInteger(),
                    ecPointFrom.affineX,
                    ecPointFrom.affineY,
                    ecPointTo.affineX,
                    ecPointTo.affineY,
                    request.requestData.toByteArray(),
                    request.responseData.toByteArray(),
                    request.state.ordinal.toBigInteger()
            ).send()

            for (log in tx.logs) {
                if (log.topics[0].substring(2) == "RequestDataCreated(uint256)".sha3().hex()) {
                    return RequestData(
                            log.topics[1].substring(2).toLong(16),
                            request.fromPk,
                            request.toPk,
                            request.requestData,
                            request.responseData,
                            request.state
                    )
                }
            }
        } catch (e: Exception) {
            System.out.println(e.localizedMessage)
        }

        return request
    }

    override fun deleteAccount(publicKey: String): Long
    {
        throw DataNotSaved();
    }

    private fun tupleToRequestData(tuple: Tuple8<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, ByteArray, ByteArray, BigInteger>): RequestData {
        return RequestData(
                tuple.value1.toLong(),
                ECPoint(tuple.value2, tuple.value3).compressedString(),
                ECPoint(tuple.value4, tuple.value5).compressedString(),
                tuple.value6.toString(Charset.defaultCharset())
                        .trim(Character.MIN_VALUE),
                tuple.value7.toString(Charset.defaultCharset())
                        .trim(Character.MIN_VALUE),
                RequestData.RequestDataState.values()[tuple.value8.toInt()])
    }

}