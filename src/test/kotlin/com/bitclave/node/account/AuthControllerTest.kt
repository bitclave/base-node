package com.bitclave.node.account

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
/*
import com.bitclave.node.configuration.properties.EthereumProperties
import org.junit.runner.RunWith
import org.junit.Test
*/
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/*
import com.bitclave.node.repository.account.AccountRepository
import com.bitclave.node.repository.data.ClientDataRepository

// Ethereum Implementation
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.repository.account.EthAccountRepositoryImpl
import com.bitclave.node.repository.data.EthClientDataRepositoryImpl
import org.junit.Before
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.Web3j
import org.web3j.tx.Contract.GAS_LIMIT
import org.web3j.tx.ManagedTransaction.GAS_PRICE
*/

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
/*
    private lateinit var accountRepository: AccountRepository
    private lateinit var clientDataRepository: ClientDataRepository

    // Ethereum Implementation
    @Before
    fun before() {
        val web3 = Web3j.build(HttpService(EthereumProperties().nodeUrl))
        val credentials = Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3") // First PrivKey from ganache-cli
        val accountContract = AccountContract.deploy(web3, credentials, GAS_PRICE, GAS_LIMIT, "0x0").send()
        accountRepository = EthAccountRepositoryImpl(accountContract)
        clientDataRepository = EthClientDataRepositoryImpl(accountContract)
    }

    @Test
    fun whenCalledShouldReturnHello() {
        accountRepository.findByPublicKey("123");
        //assertNotNull(result)
        //assertEquals(result?.statusCode, HttpStatus.OK)
        //assertEquals(result?.body, "hello world")
    }
*/

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var account: Account
    protected lateinit var requestAccount: SignedRequest<Account>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        account = Account(publicKey)
        requestAccount = SignedRequest<Account>(account, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
    }

    @Test
    fun registration() {
        this.mvc.perform(post("/registration")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test
    fun existAccount() {
        this.mvc.perform(post("/exist")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
