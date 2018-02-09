package com.bitclave.node.repository.account

import java.math.BigInteger

import com.bitclave.node.repository.models.Account
import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.stereotype.Component

@Component
class EthereumAccountRepositoryImpl(val contract: AccountContract) : AccountRepository {

    override fun saveAccount(id: String, publicKey: String) {
        var tx = contract.save(BigInteger(id), BigInteger(publicKey), BigInteger(publicKey)).send()
    }

    override fun findById(id: String): Account? {
        return Account("", ""+contract.publicKeyXById(BigInteger(id))+contract.publicKeyYById(BigInteger(id)))
    }

}
