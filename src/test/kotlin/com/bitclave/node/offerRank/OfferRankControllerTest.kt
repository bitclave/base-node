package com.bitclave.node.offerRank

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferRank
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class OfferRankControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String
    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var requestOfferRank: SignedRequest<OfferRank>
    private var httpHeaders: HttpHeaders = HttpHeaders()
    private val offerRank = OfferRank(0, 10, 1234567, 1)

    @Before
    fun setup() {
        version = "v1"
        requestOfferRank = SignedRequest(offerRank, publicKey)
        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `create offer`() {
        this.mvc.perform(
            MockMvcRequestBuilders.put("/$version/offerRank/")
                .content(requestOfferRank.toJsonString())
                .headers(httpHeaders)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }
}
