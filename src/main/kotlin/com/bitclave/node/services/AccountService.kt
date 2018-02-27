package com.bitclave.node.services

import com.bitclave.node.extensions.validateSig
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AccountService(private val accountRepository: AccountRepositoryStrategy) {

    init {
        accountRepository.changeStrategy(RepositoryType.POSTGRES)
    }

    fun checkSigMessage(request: SignedRequest<*>): CompletableFuture<String> {
        return request.validateSig()
                .thenApply { isValid ->
                    if (!isValid) {
                        throw AccessDeniedException()
                    }

                    request.pk
                }
    }

    fun accountBySigMessage(request: SignedRequest<*>): CompletableFuture<Account> {
        return checkSigMessage(request)
                .thenApply(accountRepository::findByPublicKey)
                .thenApply { account: Account? ->
                    if (account == null) {
                        throw NotFoundException()
                    }

                    if (request.data == null) {
                        throw BadArgumentException()
                    }
                    account
                }
    }

    fun registrationClient(account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            if (!account.isValid()) {
                throw BadArgumentException()
            }

            if (accountRepository.findByPublicKey(account.publicKey) != null) {
                throw AlreadyRegisteredException()
            }

            accountRepository.saveAccount(account.publicKey)

            account
        }
    }

    fun existAccount(account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            accountRepository.findByPublicKey(account.publicKey) ?: throw NotFoundException()
        }
    }

}
