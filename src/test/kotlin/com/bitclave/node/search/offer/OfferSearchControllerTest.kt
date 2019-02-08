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
    protected lateinit var offerEventRequest: SignedRequest<String>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val offerSearchModel = OfferSearch(
            0,
            publicKey,
           1L,
            1L,
            OfferResultAction.NONE,
            "",
            "",
            ArrayList()
    )

    @Before fun setup() {
        version = "v1"

        offerSearchRequest = SignedRequest(offerSearchModel, publicKey)
        offerSearchIdRequest = SignedRequest(1L, publicKey)
        offerEventRequest = SignedRequest("bla-bla-bla", publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `get offer search list by searchRequestId`() {
        this.mvc.perform(get("/$version/search/result/")
                .param("searchRequestId", "1")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offer search list by offerSearchId`() {
        this.mvc.perform(get("/$version/search/result/")
                .param("offerSearchId", "1")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get dangling offer search list by offer`() {
        this.mvc.perform(get("/$version/search/result/byOffer")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get dangling offer search list by searchRequest`() {
        this.mvc.perform(get("/$version/search/result/bySearchRequest")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offerSearches with the same owner and offerId but different content`() {
        this.mvc.perform(get("/$version/search/result/conflicted")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get the total count of OfferSearches`() {
        this.mvc.perform(get("/$version/search/result/count")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offer search list by owner`() {
        this.mvc.perform(get("/$version/search/result/user")
                .param("owner", publicKey)
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `add event`() {
        this.mvc.perform(patch("/$version/search/result/event/1")
                .content(offerEventRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `complain to search result`() {
        this.mvc.perform(patch("/$version/search/result/1")
                .content(offerSearchIdRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `add offer search item`() {
        this.mvc.perform(post("/$version/search/result/")
                .content(offerSearchRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `get offer search by page`() {
        this.mvc.perform(get("/$version/search/results?page=0&size=2")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }
}
