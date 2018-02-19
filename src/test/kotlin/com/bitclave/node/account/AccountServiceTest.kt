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
import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.account.EthAccountRepositoryImpl
import com.bitclave.node.solidity.generated.AccountContract
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.Web3j
import org.web3j.tx.Contract.GAS_LIMIT
import org.web3j.tx.ManagedTransaction.GAS_PRICE

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
        val web3 = Web3j.build(HttpService(EthereumProperties().nodeUrl))
        val credentials = Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3") // First PrivKey from ganache-cli
        val accountContract = AccountContract.deploy(web3, credentials, GAS_PRICE, GAS_LIMIT, "0x0").send()

        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val ethereum = EthAccountRepositoryImpl(accountContract)
        val strategy = AccountRepositoryStrategy(postgres, ethereum)
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
