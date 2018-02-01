package com.bitclave.node.services

import com.bitclave.node.configuration.properties.AccountProperties
import com.bitclave.node.repository.account.AccountRepository
import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.Sha3Utils
import javassist.tools.web.BadHttpRequest
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AccountService(private val accountRepository: AccountRepository,
        private val accountProperties: AccountProperties) {

    fun registrationClient(account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            if (!account.isValid()) {
                throw BadHttpRequest()
            }

            val clientId = generateClientId(accountProperties.salt, account.hash)

            if (accountRepository.findById(clientId) != null) {
                throw AlreadyRegisteredException()
            }

            accountRepository.saveAccount(clientId, account.publicKey)

            Account(clientId)
        }
    }

    fun authorization(account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            val clientId = generateClientId(accountProperties.salt, account.hash)
            accountRepository.findById(clientId) ?: throw NotFoundException()
        }
    }

    private fun generateClientId(salt: String, hash: String): String {
        return Sha3Utils.stringToSha3Hex("$salt$hash")
    }

}
