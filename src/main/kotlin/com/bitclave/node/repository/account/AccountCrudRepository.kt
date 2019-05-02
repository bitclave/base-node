package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
@Transactional
interface AccountCrudRepository : CrudRepository<Account, String> {

    fun findByPublicKey(key: String): Account?

    fun findAllByPublicKeyIn(key: List<String>): List<Account>

    fun deleteByPublicKey(key: String)

    fun findByCreatedAtAfter(createdAt: Date): List<Account>
}
