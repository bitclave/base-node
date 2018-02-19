package com.bitclave.node.account

import com.bitclave.node.extensions.signMessage
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.NotFoundException
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountServiceTest {

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository
    protected lateinit var accountService: AccountService

    protected val privateKey = "c9574c6138fe689946e4f0273e848a8219a6652288273dc6cf291e09517d0abd"
    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    protected lateinit var account: Account

    @Before
    fun setup() {
        account = Account(publicKey)
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val strategy = AccountRepositoryStrategy(postgres)
        accountService = AccountService(strategy)
        strategy.changeStrategy(RepositoryType.POSTGRES)
    }

    @Test
    fun checkSigMessage() {
        val request = SignedRequest("Hello", publicKey)
        request.signMessage(privateKey)

        val signPublicKey = accountService.checkSigMessage(request).get()
        Assertions.assertThat(signPublicKey).isEqualTo(publicKey)
    }

    @Test
    fun accountBySigMessage() {
        accountService.registrationClient(account).get()
        val request = SignedRequest("Hello", publicKey)
        request.signMessage(privateKey)

        val account = accountService.accountBySigMessage(request).get()
        Assertions.assertThat(account.publicKey).isEqualTo(publicKey)
    }

    @Test
    fun registration() {
        val regAccount = accountService.registrationClient(account).get()
        Assertions.assertThat(regAccount.publicKey).isEqualTo(publicKey)
    }

    @Test(expected = AlreadyRegisteredException::class)
    fun isAlreadyRegistered() {
        accountService.registrationClient(account).get()
        try {
            accountService.registrationClient(account).get()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test
    fun existAccount() {
        accountService.registrationClient(account).get()
        val existAccount = accountService.existAccount(account).get()
        Assertions.assertThat(existAccount.publicKey).isEqualTo(publicKey)
    }

    @Test(expected = NotFoundException::class)
    fun notExistAccount() {
        try {
            accountService.existAccount(account).get()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

}
