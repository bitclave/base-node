package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(account: Account) {
        repository.save(account) ?: throw DataNotSaved()
    }

    override fun deleteAccount(publicKey: String): Long {
        return repository.deleteByPublicKey(publicKey)
    }

    override fun findByPublicKey(publicKey: String): Account? {
        return repository.findByPublicKey(publicKey)
    }

}
