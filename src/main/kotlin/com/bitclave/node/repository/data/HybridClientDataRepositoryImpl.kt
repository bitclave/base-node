package com.bitclave.node.repository.data

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.ECPoint
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.services.errors.NotImplementedException
import com.bitclave.node.solidity.generated.ClientDataContract
import com.bitclave.node.solidity.generated.NameServiceContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.charset.Charset

@Component
@Qualifier("hybrid")
class HybridClientDataRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val hybridProperties: HybridProperties
) : ClientDataRepository {

    var allKeysArr: Array<String> = emptyArray()

    private val nameServiceData = hybridProperties.contracts.nameService
    private lateinit var nameServiceContract: NameServiceContract
    private lateinit var contract: ClientDataContract

    init {
        nameServiceContract = NameServiceContract.load(
                nameServiceData.address,
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )

        contract = ClientDataContract.load(
                nameServiceContract.addressOfName("clientData").send(),
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )
    }

    override fun allKeys(): Array<String> {
        if (allKeysArr.isEmpty()) {
            val keysCount = contract.keysCount().send().toLong()
            allKeysArr = (0..(keysCount - 1))
                    .map {
                        deserializeKey(contract.keys(it.toBigInteger()).send())
                    }.toTypedArray()
        }
        return allKeysArr
    }

    override fun getData(publicKey: String): Map<String, String> {
        val ecPoint = ECPoint(publicKey)
        return getClientKeys(ecPoint.affineX).map {
            it to contract.info(ecPoint.affineX, serializeKey(it)).send()
        }.toMap<String, String>()
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        val ecPoint = ECPoint(publicKey)

        for (entry in data) {
            val oldValue = contract.info(ecPoint.affineX, serializeKey(entry.key)).send();
            if (oldValue != entry.value) {
                contract.setInfo(ecPoint.affineX, serializeKey(entry.key), entry.value).send()
            }
        }
        allKeysArr = emptyArray()
        getData(publicKey)
    }

    override fun deleteData(publicKey: String) {
        val ecPoint = ECPoint(publicKey)
        getClientKeys(ecPoint.affineX).map {
            contract.deleteInfo(ecPoint.affineX, serializeKey(it)).send()
        }
    }

    private fun serializeKey(key: String): ByteArray {
        return key.padEnd(32, Character.MIN_VALUE).toByteArray();
    }

    private fun deserializeKey(keyData: ByteArray): String {
        return keyData.toString(Charset.defaultCharset())
                .trim(Character.MIN_VALUE)
    }

    private fun getClientKeys(publicKeyX: BigInteger): Array<String> {
        val keysCount = contract.clientKeysCount(publicKeyX).send().toLong()
        return (0..(keysCount - 1)).map {
            deserializeKey(contract.clientKeys(publicKeyX, it.toBigInteger()).send())
        }.toTypedArray()
    }

}
