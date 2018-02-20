package com.bitclave.node.repository.account

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.Account
import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("ethereum")
class EthAccountRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val ethereumProperties: EthereumProperties
) : AccountRepository {

    private val contractData = ethereumProperties.contracts.account

    private var contract = AccountContract.load(
            contractData.address,
            web3Provider.web3,
            web3Provider.credentials,
            contractData.gasPrice,
            contractData.gasLimit
    )

    override fun saveAccount(publicKey: String) {
        contract.registerPublicKey(publicKey).send()
    }

    override fun findByPublicKey(publicKey: String): Account? {
        if (contract.isRegisteredPublicKey(publicKey).send()) {
            return Account(publicKey)
        }
        return null
    }

}
