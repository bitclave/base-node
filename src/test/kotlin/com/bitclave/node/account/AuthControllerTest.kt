package com.bitclave.node.account

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String
    protected lateinit var account: Account
    protected lateinit var requestAccount: SignedRequest<Account>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before fun setup() {
        version = "v1"
        account = Account(publicKey)
        requestAccount = SignedRequest<Account>(account, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `check registration`() {
        this.mvc.perform(post("/$version/registration")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `check account is exist`() {
        this.mvc.perform(post("/$version/exist")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `delete account`() {
        this.mvc.perform(delete("/$version/delete")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get nonce`() {
        this.mvc.perform(get("/$version/nonce/$publicKey")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
