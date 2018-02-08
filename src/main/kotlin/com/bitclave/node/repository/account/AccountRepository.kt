package com.bitclave.node.repository.account

import com.bitclave.node.repository.models.Account

interface AccountRepository {

    fun saveAccount(publicKey: String)

    fun findByPublicKey(key: String): Account?

}
