package com.bitclave.node.repository.data

import com.bitclave.node.extensions.hex
import com.bitclave.node.extensions.sha3
import java.math.BigInteger

import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("ethereum")
class EthClientDataRepositoryImpl(val repository: AccountContract) :
        ClientDataRepository {

    companion object {
       val allKeys: Array<String> = arrayOf("gender", "age")
    }

    fun readValueForKey(publicKey: String, key: String): String {
        var value = ""
        val hash = BigInteger(key.sha3().hex(), 16)
        var i = BigInteger.valueOf(0)
        while (true) {
            val item = repository.info(publicKey, hash, i).send().toString()
            if (item.isEmpty()) {
                break
            }
            value += item
            i++
        }
        return value
    }

    override fun getData(publicKey: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (key in allKeys) {
            map[key] = readValueForKey(publicKey, key)
        }
        return map
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        for (key in data.keys) {
            val hash = BigInteger(key.sha3().hex(), 16)
            val oldValue = readValueForKey(publicKey, key)
            if (oldValue != data[key]) {
                val arr = data[key]!!.toByteArray().asList().chunked(32).map { a -> a.toTypedArray().toByteArray() }
                repository.setInfos(publicKey, hash, arr).send()
            }
        }
    }

}
