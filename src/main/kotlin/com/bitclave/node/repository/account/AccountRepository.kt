package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account
import java.util.*

interface AccountRepository {

    fun saveAccount(account: Account)

    fun findByPublicKey(publicKey: String): Account?

    fun findByPublicKey(publicKeys: List<String>): List<Account>

    fun findByCreatedAtAfter(createdAt: Date): List<Account>

    fun deleteAccount(publicKey: String)

    fun getTotalCount(): Long
}
