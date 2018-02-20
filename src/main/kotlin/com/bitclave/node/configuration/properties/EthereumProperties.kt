package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
@ConfigurationProperties("ethereum")
data class EthereumProperties(
        var nodeUrl: String = "",
        var ownerPrivateKey: String = "",
        var contracts: EthereumContracts = EthereumContracts()
)

data class EthereumContracts(
        var account: EthereumContractData = EthereumContractData(),
        var storage: EthereumContractData = EthereumContractData()
)

data class EthereumContractData(
        var address: String = "",
        var gasPrice: BigInteger = BigInteger.ZERO,
        var gasLimit: BigInteger = BigInteger.ZERO)
