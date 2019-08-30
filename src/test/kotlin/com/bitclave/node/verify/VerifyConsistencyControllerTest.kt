package com.bitclave.node.verify

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.models.SignedRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class VerifyConsistencyControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val publicKey2 = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"
    private val fromDate = Date()
    protected lateinit var idsRequest: SignedRequest<List<Long>>
    protected lateinit var publicKeysRequest: SignedRequest<List<String>>
    protected lateinit var fromDateRequest: SignedRequest<Long>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val ids = mutableListOf(1L, 2L, 3L, 4L)
    private val publicKeys = mutableListOf(publicKey, publicKey2)

    @Before
    fun setup() {

        idsRequest = SignedRequest(ids, publicKey)
        publicKeysRequest = SignedRequest(publicKeys, publicKey)
        publicKeysRequest = SignedRequest(publicKeys, publicKey)
        fromDateRequest = SignedRequest(fromDate.time)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `get offer search list by ids`() {
        this.mvc.perform(
            post("/dev/verify/offersearch/ids")
                .content(idsRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get account list by publicKeys`() {
        this.mvc.perform(
            post("/dev/verify/account/publickeys")
                .content(publicKeysRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `expected error - get account list by publicKeys`() {
        this.mvc.perform(
            post("/dev/verify/account/publickeys")
                .content(publicKey)
                .headers(httpHeaders)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `get all accounts`() {
        this.mvc.perform(
            post("/dev/verify/account/all")
                .content(fromDateRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get dangling offer search list by searchRequest`() {
        this.mvc.perform(
            get("/dev/verify/offersearch/dangling/1")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offerSearches with the same owner and offerId but different content`() {
        this.mvc.perform(
            get("/dev/verify/offersearch/conflicted")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get dangling offer interaction list`() {
        this.mvc.perform(
            get("/dev/verify/offerinteraction/dangling")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }
}
