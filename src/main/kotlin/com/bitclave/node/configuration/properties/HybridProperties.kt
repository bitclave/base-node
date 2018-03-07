package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
@ConfigurationProperties("hybrid")
data class HybridProperties(
        var nodeUrl: String = "",
        var ownerPrivateKey: String = "",
        var contracts: HybridContracts = HybridContracts()
)

data class HybridContracts(
        var nameService: HybridContractData = HybridContractData()
)

data class HybridContractData(
        var address: String = "",
        var gasPrice: BigInteger = BigInteger.ZERO,
        var gasLimit: BigInteger = BigInteger.ZERO)
