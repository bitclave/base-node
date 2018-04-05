package com.bitclave.node.repository.account

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.NotImplementedException
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.NameServiceContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("hybrid")
class HybridAccountRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val hybridProperties: HybridProperties
) : AccountRepository {

    private val nameServiceData = hybridProperties.contracts.nameService
    private lateinit var nameServiceContract: NameServiceContract
    private lateinit var contract: AccountContract

    init {
        nameServiceContract = NameServiceContract.load(
                nameServiceData.address,
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )

        contract = AccountContract.load(
                nameServiceContract.addressOfName("account").send(),
                web3Provider.web3,
                web3Provider.credentials,
                nameServiceData.gasPrice,
                nameServiceData.gasLimit
        )
    }

    override fun saveAccount(publicKey: String) {
        contract.registerPublicKey(publicKey).send()
    }

    override fun deleteAccount(publicKey: String): Long {
        throw NotImplementedException()
    }

    override fun findByPublicKey(publicKey: String): Account? {
        if (contract.isRegisteredPublicKey(publicKey).send()) {
            return Account(publicKey)
        }
        return null
    }

}
