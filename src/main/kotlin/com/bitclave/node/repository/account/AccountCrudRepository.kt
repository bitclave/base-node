package com.bitclave.node.repository.account

import com.bitclave.node.repository.entities.Account
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Repository
@Transactional
interface AccountCrudRepository : CrudRepository<Account, String> {

    @Transactional(readOnly = true)
    fun findAllBy(pageable: Pageable): Slice<Account>

    @Transactional(readOnly = true)
    fun findByPublicKey(key: String): Account?

    @Transactional(readOnly = true)
    fun findAllByPublicKeyIn(key: List<String>): List<Account>

    fun deleteByPublicKey(key: String)

    @Transactional(readOnly = true)
    fun findByCreatedAtAfter(createdAt: Date): List<Account>
}
