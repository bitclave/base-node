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
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock

private val logger = KotlinLogging.logger {}

@Service
@Qualifier("v1")
class AccountService(private val accountRepository: RepositoryStrategy<AccountRepository>) {

    private val nonceIncrementLock = ReentrantLock(true)

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

        try {
            return checkSigMessage(request)
                .thenApply(accountRepository.changeStrategy(strategy)::findByPublicKey)
                .thenApply { account: Account? ->

                    if (account == null) {
                        throw NotFoundException("Account not found")
                    }

                    if (request.data == null) {
                        throw BadArgumentException()
                    }
                    account
                }
        } catch (e: Exception) {
            logger.error("Request: $request raised $e")
            throw BadArgumentException(e.localizedMessage)
        }
    }

    fun validateNonce(request: SignedRequest<*>, account: Account): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            if (request.nonce != account.nonce + 1) {
                throw BadArgumentException()
            }
            account
        }
    }

    fun incrementNonce(
        account: Account,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        // todo for optimization we can separate lock for each client by unique public key.
        return CompletableFuture.runAsync {
            nonceIncrementLock.lock()

            try {
                val repo = accountRepository.changeStrategy(strategy)
                val actualAccount = repo.findByPublicKey(account.publicKey)
                    ?: throw NotFoundException("Account not found")

                actualAccount.nonce++

                repo.saveAccount(actualAccount)
            } finally {
                nonceIncrementLock.unlock()
            }
        }
    }

    fun getNonce(publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            val account = accountRepository.changeStrategy(strategy).findByPublicKey(publicKey)

            account?.nonce ?: 0
        }
    }

    fun registrationClient(account: Account, strategy: RepositoryStrategyType): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            if (!account.isValid()) {
                throw BadArgumentException()
            }
            accountRepository.changeStrategy(strategy)
            if (accountRepository.changeStrategy(strategy)
                    .findByPublicKey(account.publicKey) != null
            ) {
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
                .findByPublicKey(account.publicKey) ?: throw NotFoundException(
                "User with baseID ${account.publicKey} does not exist"
            )
        }
    }

    fun deleteAccount(clientId: String, strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            accountRepository.changeStrategy(strategy)
                .deleteAccount(clientId)
        }
    }

    fun getAccounts(
        strategy: RepositoryStrategyType,
        publicKeys: List<String>
    ): CompletableFuture<List<Account>> {

        return CompletableFuture.supplyAsync {
            accountRepository.changeStrategy(strategy)
                .findByPublicKey(publicKeys)
        }
    }

    fun getAllAccounts(
        strategy: RepositoryStrategyType,
        fromDate: Date
    ): CompletableFuture<List<Account>> {

        return CompletableFuture.supplyAsync {
            accountRepository.changeStrategy(strategy)
                .findByCreatedAtAfter(fromDate)
        }
    }

    fun getAccountTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync {

            val repository = accountRepository.changeStrategy(strategy)

            return@supplyAsync repository.getTotalCount()
        }
    }
}
