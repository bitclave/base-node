package com.bitclave.node.repository.data

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.fromHex
import com.bitclave.node.extensions.hex
import com.bitclave.node.extensions.sha3
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.ClientDataContract
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

    private val contractData = hybridProperties.contracts.clientData

    private val contract = ClientDataContract.load(
            contractData.address,
            web3Provider.web3,
            web3Provider.credentials,
            contractData.gasPrice,
            contractData.gasLimit
    )

    override fun allKeys(): Array<String> {
        if (allKeysArr.isEmpty()) {
            allKeysArr = (0..(contract.keysCount().send().toLong() - 1))
                    .map {
                        contract.keys(BigInteger.valueOf(it)).send()
                    }
                    .map {
                        it.toString(Charset.defaultCharset())
                          .trim(Character.MIN_VALUE)
                    }.toTypedArray()
        }
        return allKeysArr
    }

    override fun getData(publicKey: String): Map<String, String> {
        return allKeys().map {
            it to readValueForKey(publicKey, it)
        }.toMap().filter {
            !it.value.isEmpty()
        }
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        for (entry in data) {
            val oldValue = readValueForKey(publicKey, entry.key)

            if (oldValue != entry.value) {
                var padLength = ((entry.value.length + 32 - 1) / 32) * 32
                val validValue = entry.value.padEnd(padLength, Character.MIN_VALUE)

                val arr = validValue.toByteArray()
                        .asList()
                        .chunked(32)
                        .map { it.toByteArray() }
                contract.setInfos(publicKey, entry.key.padEnd(32, Character.MIN_VALUE).toByteArray(), arr).send()
            }
        }
    }

    private fun readValueForKey(publicKey: String, key: String): String {
        val stringBuilder = StringBuilder()
        var i = BigInteger.valueOf(0)
        while (true) {
            val item = contract.info(publicKey, key.padEnd(32, Character.MIN_VALUE).toByteArray(), i)
                    .send()
                    .toString(Charset.defaultCharset())
                    .trim(Character.MIN_VALUE)

            if (item.isEmpty()) {
                break
            }
            stringBuilder.append(item)
            i++
        }
        return stringBuilder.toString()
    }

}
