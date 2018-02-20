package com.bitclave.node.repository

import com.bitclave.node.configuration.properties.EthereumProperties
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService

@Component
class Web3Provider(private val ethereumProperties: EthereumProperties) {

    private val service = HttpService(ethereumProperties.nodeUrl)

    val web3: Web3j = Web3j.build(service)

    val credentials: Credentials = Credentials.create(ethereumProperties.ownerPrivateKey)

    fun ethRevert() {
        Request<Int, EthRevert>(
                "evm_revert",
                arrayListOf(1),
                service,
                EthRevert::class.java
        ).send()
    }

    fun ethSnapshot() {
        Request<String, EthSnapshot>(
                "evm_snapshot",
                emptyList(),
                service,
                EthSnapshot::class.java
        ).send()
    }

    private class EthRevert : Response<Boolean>()
    private class EthSnapshot : Response<String>()

}
