package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.DataNotSaved
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresAccountRepositoryImpl(val repository: AccountCrudRepository) : AccountRepository {

    override fun saveAccount(publicKey: String) {
        repository.save(Account(publicKey)) ?: throw DataNotSaved()
    }

    override fun deleteAccount(publicKey: String) {
        repository.findByPublicKey(publicKey) ?: throw NotFoundException();
        repository.delete(Account(publicKey)) ?: throw DataNotSaved();
    }

    override fun findByPublicKey(publicKey: String): Account? {
        return repository.findByPublicKey(publicKey)
    }

}
