package com.bitclave.node.repository.account

import com.bitclave.node.repository.entities.Account
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Date

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

    override fun findAll(pageable: Pageable): Slice<Account> = repository.findAllBy(pageable)

    override fun findByPublicKey(publicKeys: List<String>): List<Account> {
        return repository.findAllByPublicKeyIn(publicKeys)
            .asSequence()
            .toList()
    }

    override fun findByCreatedAtAfter(createdAt: Date): List<Account> {
        return repository.findByCreatedAtAfter(createdAt)
            .asSequence()
            .toList()
    }

    @Transactional(readOnly = true)
    override fun getTotalCount(): Long {
        return repository.count()
    }
}
