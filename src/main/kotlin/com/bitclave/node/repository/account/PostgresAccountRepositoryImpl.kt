package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(account: Account) {
        repository.save(account) ?: throw DataNotSavedException()
    }

    override fun deleteAccount(publicKey: String) {
        repository.deleteByPublicKey(publicKey)
    }

    override fun findByPublicKey(publicKey: String): Account? {
        return repository.findByPublicKey(publicKey)
    }

}
