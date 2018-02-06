package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.stereotype.Component

@Component
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(id: String, publicKey: String) {
        repository.save(Account(id, publicKey)) ?: throw DataNotSaved()
    }

    override fun findById(id: String): Account? {
        return repository.findOne(id)
    }

}
