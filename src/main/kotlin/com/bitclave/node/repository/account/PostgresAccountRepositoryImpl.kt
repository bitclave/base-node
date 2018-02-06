package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import org.springframework.stereotype.Component

@Component
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(id: String, publicKey: String) {
        repository.save(Account(id, publicKey)) ?: throw RuntimeException("data not saved")
    }

    override fun findById(id: String): Account? {
        return repository.findOne(id)
    }

}
