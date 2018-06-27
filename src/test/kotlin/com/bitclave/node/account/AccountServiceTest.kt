package com.bitclave.node.account

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.signMessage
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.errors.AlreadyRegisteredException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.services.v1.AccountService
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
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    private lateinit var accountCrudRepository: AccountCrudRepository

    protected lateinit var accountService: AccountService

    protected val privateKey = "c9574c6138fe689946e4f0273e848a8219a6652288273dc6cf291e09517d0abd"
    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    protected lateinit var account: Account
    protected lateinit var strategy: RepositoryStrategyType

    @Before fun setup() {
        account = Account(publicKey)
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)

        accountService = AccountService(repositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
    }

    @Test fun `check nonce`() {
        accountService.registrationClient(account, strategy).get()
        var nonce = accountService.getNonce(account.publicKey, strategy).get()

        val request = SignedRequest("Hello", publicKey, "", ++nonce)
        request.signMessage(privateKey)

        val account = accountService.accountBySigMessage(request, strategy).get()
        accountService.validateNonce(request, account).get()
    }

    @Test(expected = BadArgumentException::class)
    fun `check invalid nonce`() {
        try {
            accountService.registrationClient(account, strategy).get()
            var nonce = accountService.getNonce(account.publicKey, strategy).get()

            val request = SignedRequest("Hello", publicKey, "", ++nonce)
            request.signMessage(privateKey)

            var account = accountService.accountBySigMessage(request, strategy).get()
            accountService.incrementNonce(account, strategy).get()

            account = accountService.accountBySigMessage(request, strategy).get()

            accountService.validateNonce(request, account).get()

        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test fun `check signature of signed message`() {
        val request = SignedRequest("Hello", publicKey)
        request.signMessage(privateKey)

        val signPublicKey = accountService.checkSigMessage(request).get()
        Assertions.assertThat(signPublicKey).isEqualTo(publicKey)
    }

    @Test fun `get account by signature of message`() {
        accountService.registrationClient(account, strategy).get()
        val request = SignedRequest("Hello", publicKey)
        request.signMessage(privateKey)

        val account = accountService.accountBySigMessage(request, strategy).get()
        Assertions.assertThat(account.publicKey).isEqualTo(publicKey)
    }

    @Test fun `register new client`() {
        val regAccount = accountService.registrationClient(account, strategy).get()
        Assertions.assertThat(regAccount.publicKey).isEqualTo(publicKey)
    }

    @Test fun `delete client`() {
        val regAccount = accountService.registrationClient(account, strategy).get()
        Assertions.assertThat(regAccount.publicKey).isEqualTo(publicKey)
        accountService.deleteAccount(account.publicKey, strategy).get()

        accountService.registrationClient(account, strategy).get()
    }

    @Test(expected = AlreadyRegisteredException::class)
    fun `expect error - already registered client`() {
        accountService.registrationClient(account, strategy).get()
        try {
            accountService.registrationClient(account, strategy).get()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test fun `check client already registered`() {
        accountService.registrationClient(account, strategy).get()
        val existAccount = accountService.existAccount(account, strategy).get()
        Assertions.assertThat(existAccount.publicKey).isEqualTo(publicKey)
    }

    @Test(expected = NotFoundException::class)
    fun `expect error - client not existed`() {
        try {
            accountService.existAccount(account, strategy).get()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

}
