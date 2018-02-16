package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(publicKey: String) {
        repository.save(Account(publicKey)) ?: throw DataNotSaved()
    }

    override fun findByPublicKey(key: String): Account? {
        return repository.findByPublicKey(key)
    }

}
