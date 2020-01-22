package com.bitclave.node

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.NameServiceContract
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import java.math.BigInteger

@Component
class ContractLoader(val hybridProperties: HybridProperties) {

    val web3Provider: Web3Provider by lazy {
        Web3Provider(hybridProperties)
    }

    final inline fun <reified T> loadContract(contractName: String): T {
        val nameServiceData = hybridProperties.contracts.nameService

        val nameServiceContract = NameServiceContract.load(
            nameServiceData.address,
            web3Provider.web3,
            web3Provider.credentials,
            nameServiceData.gasPrice,
            nameServiceData.gasLimit
        )

        val ctr = T::class.java.getDeclaredConstructor(
            String::class.java,
            Web3j::class.java,
            Credentials::class.java,
            BigInteger::class.java,
            BigInteger::class.java
        )
        // it hack need because generated class has protected constructor. will think more about how to call it right.
        ctr.isAccessible = true

        return ctr.newInstance(
            nameServiceContract.addressOfName(contractName).send(),
            web3Provider.web3,
            web3Provider.credentials,
            nameServiceData.gasPrice,
            nameServiceData.gasLimit
        )
    }
}
