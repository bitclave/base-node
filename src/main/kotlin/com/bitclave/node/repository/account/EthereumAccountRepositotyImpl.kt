package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
//import com.bitclave.node.solidity.AccountContract
import org.springframework.stereotype.Component

@Component
class EthereumAccountRepositoryImpl(val contract: Account) : AccountRepository {

    override fun saveAccount(id: String, publicKey: String) {
        //var tx = contract.save(id, publicKey).send()
    }

    override fun findById(id: String): Account? {
        //return contract.publicKeyXById(id) + contract.publicKeyYById(id)
        return null
    }

}
