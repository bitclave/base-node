package com.bitclave.node.repository.account

import com.bitclave.node.repository.entities.Account
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.Date

interface AccountRepository {

    fun saveAccount(account: Account)

    fun findByPublicKey(publicKey: String): Account?

    fun findAll(pageable: Pageable): Slice<Account>

    fun findByPublicKey(publicKeys: List<String>): List<Account>

    fun findByCreatedAtAfter(createdAt: Date): List<Account>

    fun deleteAccount(publicKey: String)

    fun getTotalCount(): Long
}
