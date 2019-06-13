package com.bitclave.node.search.offer

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.repository.models.controllers.OfferSearchByQueryParameters
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.ArrayList
import java.util.Date

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
    protected lateinit var requestSearch: SignedRequest<SearchRequest>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val offerSearchModel = OfferSearch(
        0,
        publicKey,
        1L,
        1L,
        OfferResultAction.NONE,
        "",
        ArrayList(),
        Date(1550561756503)
    )

    private val searchRequest = SearchRequest(
        0,
        publicKey,
        mapOf("car" to "true", "color" to "red")
    )

    @Before
    fun setup() {
        version = "v1"

        offerSearchRequest = SignedRequest(offerSearchModel, publicKey)
        offerSearchIdRequest = SignedRequest(1L, publicKey)
        offerEventRequest = SignedRequest("bla-bla-bla", publicKey)

        requestSearch = SignedRequest(searchRequest, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `create offerSearches request by query string`() {
        val content = SignedRequest(OfferSearchByQueryParameters(1L, listOf()))
        this.mvc.perform(
            post("/$version/search/query/")
                .content(content.toJsonString())
                .param("q", "some query string")
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `get offer search list by searchRequestId`() {
        this.mvc.perform(
            get("/$version/search/result/")
                .param("searchRequestId", "1")
                .param("page", "0")
                .param("size", "20")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by offerSearchId`() {
        this.mvc.perform(
            get("/$version/search/result/")
                .param("offerSearchId", "1")
                .param("page", "0")
                .param("size", "20")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get the total count of OfferSearches`() {
        this.mvc.perform(
            get("/$version/search/result/count")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get the total count of OfferSearches by searchRequestIds`() {
        this.mvc.perform(
            get("/$version/search/count")
                .param("ids", "1,2,3")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by owner`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by owner and searchIds`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .param("searchIds", "1,2")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by owner, searchIds and state`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .param("searchIds", "1,2")
                .param("state", "EVALUATE,ACCEPT")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by owner, searchIds, state and unique`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .param("searchIds", "1,2")
                .param("state", "EVALUATE,ACCEPT")
                .param("unique", "true")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get offer search list by owner, searchIds, state, unique, page, size`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .param("searchIds", "1,2")
                .param("state", "EVALUATE,ACCEPT")
                .param("unique", "true")
                .param("page", "0")
                .param("size", "20")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get error with wrong state value`() {
        this.mvc.perform(
            get("/$version/search/result/user")
                .param("owner", publicKey)
                .param("state", "SOME_VALUE")
                .headers(httpHeaders)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `add event`() {
        this.mvc.perform(
            patch("/$version/search/result/event/1")
                .content(offerEventRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `complain to search result`() {
        this.mvc.perform(
            patch("/$version/search/result/1")
                .content(offerSearchIdRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `add offer search item`() {
        this.mvc.perform(
            post("/$version/search/result/")
                .content(offerSearchRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `add offer search item without updatedAt field`() {
        val strModel = offerSearchRequest.toJsonString()
        val modelWithoutUpdateAt = strModel.replace(",\"updatedAt\":\"2019-02-19T10:35:56.503+0300\"", "")
        this.mvc.perform(
            post("/$version/search/result/")
                .content(modelWithoutUpdateAt)
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `get offer search by page`() {
        this.mvc.perform(
            get("/$version/search/results?page=0&size=2")
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `clone offer search of search request`() {
        this.mvc.perform(
            put("/$version/search/result/$publicKey/1")
                .content(requestSearch.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }
}
