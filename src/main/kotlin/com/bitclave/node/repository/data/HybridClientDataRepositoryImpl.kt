package com.bitclave.node.repository.data

import com.bitclave.node.ContractLoader
import com.bitclave.node.extensions.ecPoint
import com.bitclave.node.solidity.generated.ClientDataContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.charset.Charset

@Component
@Qualifier("hybrid")
class HybridClientDataRepositoryImpl(
    contractLoader: ContractLoader
) : ClientDataRepository {

    var allKeysArr = emptyList<String>()

    private val contract: ClientDataContract by lazy {
        contractLoader.loadContract<ClientDataContract>("clientData")
    }

    override fun allKeys(): List<String> {
        if (allKeysArr.isEmpty()) {
            val keysCount = contract.keysCount().send().toLong()
            allKeysArr = (0..(keysCount - 1))
                .map {
                    deserializeKey(contract.keys(it.toBigInteger()).send())
                }.toList()
        }
        return allKeysArr
    }

    override fun getData(publicKey: String): Map<String, String> {
        val ecPoint = ecPoint(publicKey)
        return getClientKeys(ecPoint.affineX).map {
            it to contract.info(ecPoint.affineX, serializeKey(it)).send()
        }.toMap<String, String>()
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        val ecPoint = ecPoint(publicKey)

        for (entry in data) {
            val oldValue = contract.info(ecPoint.affineX, serializeKey(entry.key)).send()
            if (oldValue != entry.value) {
                contract.setInfo(ecPoint.affineX, serializeKey(entry.key), entry.value).send()
            }
        }
        allKeysArr = emptyList()
        getData(publicKey)
    }

    override fun deleteData(publicKey: String) {
        val ecPoint = ecPoint(publicKey)
        getClientKeys(ecPoint.affineX).map {
            contract.deleteInfo(ecPoint.affineX, serializeKey(it)).send()
        }
    }

    private fun serializeKey(key: String): ByteArray {
        return key.padEnd(32, Character.MIN_VALUE).toByteArray()
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
