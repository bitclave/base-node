package com.bitclave.node.repository.data

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.extensions.hex
import com.bitclave.node.extensions.sha3
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.ClientDataContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.charset.Charset

@Component
@Qualifier("ethereum")
class EthClientDataRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val ethereumProperties: EthereumProperties
) : ClientDataRepository {

    var allKeysArr: Array<String> = emptyArray()

    private val contractData = ethereumProperties.contracts.account

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
                    .map { contract.keys(BigInteger.valueOf(it)).send() }
                    .map {
                        it.toString(Charset.defaultCharset())
                          .trim(Character.MIN_VALUE)
                    }.toTypedArray()
        }
        return allKeysArr
    }

    override fun getData(publicKey: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (key in allKeys()) {
            map[key] = readValueForKey(publicKey, key)
        }
        return map
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        var hash: BigInteger
        var oldValue: String
        var validValue: String

        for (entry in data) {
            hash = BigInteger(entry.key.sha3().hex(), 16)
            oldValue = readValueForKey(publicKey, entry.key)

            if (oldValue != entry.value) {

                validValue = entry.value.padEnd(32, Character.MIN_VALUE)

                val arr = validValue.toByteArray()
                        .asList()
                        .chunked(32)
                        .map { it.toByteArray() }
                contract.setInfos(publicKey, hash, arr).send()
            }
        }
    }

    private fun readValueForKey(publicKey: String, key: String): String {
        val stringBuilder = StringBuilder()
        val hash = BigInteger(key.sha3().hex(), 16)
        var i = BigInteger.valueOf(0)
        while (true) {
            val item = contract.info(publicKey, hash, i)
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
