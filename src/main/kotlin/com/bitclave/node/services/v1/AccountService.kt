package com.bitclave.node.services.v1

import com.bitclave.node.extensions.validateSig
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.account.AccountRepository
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.Logger
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

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
            Logger.error("Request: $request raised", e)
            throw BadArgumentException(e.localizedMessage)
        }
    }

    fun validateNonce(request: SignedRequest<*>, account: Account): CompletableFuture<Account> {
        return CompletableFuture.completedFuture(account)
    }

    fun incrementNonce(
        account: Account,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            account.nonce++
            accountRepository.changeStrategy(strategy).saveAccount(account)
        })
    }

    fun getNonce(publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return supplyAsyncEx(Supplier {
            val account = accountRepository.changeStrategy(strategy).findByPublicKey(publicKey)

            account?.nonce ?: 0
        })
    }

    fun registrationClient(account: Account, strategy: RepositoryStrategyType): CompletableFuture<Account> {
        return supplyAsyncEx(Supplier {
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
        })
    }

    fun existAccount(account: Account, strategy: RepositoryStrategyType): CompletableFuture<Account> {
        return supplyAsyncEx(Supplier {
            accountRepository.changeStrategy(strategy)
                .findByPublicKey(account.publicKey)
                ?: throw NotFoundException("User with baseID ${account.publicKey} does not exist")
        })
    }

    fun deleteAccount(clientId: String, strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            accountRepository.changeStrategy(strategy)
                .deleteAccount(clientId)
        })
    }

    fun getSliceAccounts(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Slice<Account>> {
        return supplyAsyncEx(Supplier {
            accountRepository.changeStrategy(strategy).findAll(page)
        })
    }

    fun getAccounts(
        strategy: RepositoryStrategyType,
        publicKeys: List<String>
    ): CompletableFuture<List<Account>> {

        return supplyAsyncEx(Supplier {
            accountRepository.changeStrategy(strategy)
                .findByPublicKey(publicKeys)
        })
    }

    fun getAllAccounts(
        strategy: RepositoryStrategyType,
        fromDate: Date
    ): CompletableFuture<List<Account>> {

        return supplyAsyncEx(Supplier {
            accountRepository.changeStrategy(strategy)
                .findByCreatedAtAfter(fromDate)
        })
    }

    fun getAccountTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            accountRepository.changeStrategy(strategy).getTotalCount()
        })
    }
}
