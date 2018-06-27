package com.bitclave.node.search.offer

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class OfferSearchControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var offerSearchRequest: SignedRequest<OfferSearch>
    protected lateinit var offerSearchIdRequest: SignedRequest<Long>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val offerSearchModel = OfferSearch(
            0,
           1L,
            1L,
            OfferResultAction.NONE
    )

    @Before fun setup() {
        version = "v1"

        offerSearchRequest = SignedRequest(offerSearchModel, publicKey)
        offerSearchIdRequest = SignedRequest(1L, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `get offer search list by searchRequestId`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/result/")
                .param("searchRequestId", "1")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offer search list by offerSearchId`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/result/")
                .param("OfferSearchId", "1")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `complain to search result`() {
        this.mvc.perform(patch("/$version/client/$publicKey/search/result/1")
                .content(offerSearchIdRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `add offer search item`() {
        this.mvc.perform(post("/dev/client/$publicKey/search/result/")
                .content(offerSearchRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

}
