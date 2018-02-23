package com.bitclave.node.repository

import com.bitclave.node.configuration.properties.HybridProperties
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService

@Component
class Web3Provider(private val hybridProperties: HybridProperties) {

    private val service = HttpService(hybridProperties.nodeUrl)

    val web3: Web3j = Web3j.build(service)

    val credentials: Credentials = Credentials.create(hybridProperties.ownerPrivateKey)

    fun hybridRevert() {
        Request<Int, HybridRevert>(
                "evm_revert",
                arrayListOf(1),
                service,
                HybridRevert::class.java
        ).send()
    }

    fun hybridSnapshot() {
        Request<String, HybridSnapshot>(
                "evm_snapshot",
                emptyList(),
                service,
                HybridSnapshot::class.java
        ).send()
    }

    private class HybridRevert : Response<Boolean>()
    private class HybridSnapshot : Response<String>()

}
