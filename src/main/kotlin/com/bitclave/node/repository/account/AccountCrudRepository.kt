package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Repository
@Transactional
interface AccountCrudRepository : CrudRepository<Account, String> {

    fun findAllBy(pageable: Pageable): Slice<Account>

    fun findByPublicKey(key: String): Account?

    fun findAllByPublicKeyIn(key: List<String>): List<Account>

    fun deleteByPublicKey(key: String)

    fun findByCreatedAtAfter(createdAt: Date): List<Account>
}
