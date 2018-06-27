package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface AccountCrudRepository : CrudRepository<Account, String> {

    fun findByPublicKey(key: String): Account?

    fun deleteByPublicKey(key: String)
}
