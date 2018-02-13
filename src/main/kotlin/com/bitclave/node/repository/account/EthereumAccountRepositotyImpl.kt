package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.stereotype.Component

@Component
class EthereumAccountRepositoryImpl(val contract: AccountContract) : AccountRepository {

    override fun saveAccount(publicKey: String) {
        contract.registerPublicKey(publicKey).send()
    }

    override fun findByPublicKey(key: String): Account? {
        return Account(key)
    }

}
