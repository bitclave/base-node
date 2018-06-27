package com.bitclave.node.services.v1

import com.bitclave.node.extensions.validateSig
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.account.AccountRepository
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class AccountService(private val accountRepository: RepositoryStrategy<AccountRepository>) {

    fun checkSigMessage(request: SignedRequest<*>): CompletableFuture<String> {
        return request.validateSig()
                .thenApply { isValid ->
                    if (!isValid) {
                        throw AccessDeniedException()
                    }

                    request.pk.toLowerCase()
                }
    }

    fun accountBySigMessage(
            request: SignedRequest<*>,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Account> {

        return checkSigMessage(request)
                .thenApply(accountRepository.changeStrategy(strategy)::findByPublicKey)
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

    fun validateNonce(request: SignedRequest<*>, account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync({
            if (request.nonce != account.nonce + 1) {
                throw BadArgumentException()
            }
            account
        })
    }

    fun incrementNonce(account: Account,
                       strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            account.nonce++
            accountRepository.changeStrategy(strategy).saveAccount(account)
        })
    }

    fun getNonce(publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            val account = accountRepository.changeStrategy(strategy).findByPublicKey(publicKey)

            account?.nonce ?: 0
        })
    }

    fun registrationClient(account: Account, strategy: RepositoryStrategyType): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            if (!account.isValid()) {
                throw BadArgumentException()
            }
            accountRepository.changeStrategy(strategy)
            if (accountRepository.changeStrategy(strategy)
                            .findByPublicKey(account.publicKey) != null) {
                throw AlreadyRegisteredException()
            }

            val createdAccount = Account(account.publicKey, 1L)
            accountRepository.changeStrategy(strategy)
                    .saveAccount(createdAccount)

            createdAccount
        }
    }

    fun existAccount(account: Account, strategy: RepositoryStrategyType): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            accountRepository.changeStrategy(strategy)
                    .findByPublicKey(account.publicKey) ?: throw NotFoundException()
        }
    }

    fun deleteAccount(clientId: String, strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            accountRepository.changeStrategy(strategy)
                    .deleteAccount(clientId)
        })
    }

}
