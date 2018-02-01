package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account

interface AccountRepository {

    fun saveAccount(id: String, publicKey: String): Boolean

    fun findById(id: String): Account?

}
