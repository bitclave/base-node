package com.bitclave.node.clientData

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class ClientProfileControllerTest {

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String
    protected lateinit var requestAccount: SignedRequest<Map<String, String>>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        version = "v1"

        requestAccount = SignedRequest(emptyMap(), publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `get data`() {
        this.mvc.perform(
            get("/$version/client/$publicKey")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get data with keys`() {
        this.mvc.perform(
            get("/$version/client/$publicKey")
                .param("key", "first")
                .param("key", "second")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `update data`() {
        this.mvc.perform(
            patch("/$version/client/")
                .content(requestAccount.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }
}
