package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.stereotype.Component

@Component
class EthAccountRepositoryImpl(val contract: AccountContract) : AccountRepository {

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
