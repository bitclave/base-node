package com.bitclave.node.search

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SearchRequest
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
import java.util.*

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class SearchRequestControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var requestSearch: SignedRequest<SearchRequest>
    protected lateinit var requestSearchId: SignedRequest<Long>
    protected lateinit var requestSearchQuery: SignedRequest<String>
    protected lateinit var cloneRequestSearch: SignedRequest<SearchRequest>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val searchRequest = SearchRequest(
            0,
            publicKey,
            mapOf("car" to "true", "color" to "red"),
            Date(1550561756503),
            Date(1550561756503)
    )

    private val cloneSearchRequest = searchRequest.copy(1)

    @Before fun setup() {
        version = "v1"

        requestSearch = SignedRequest(searchRequest, publicKey)
        requestSearchId = SignedRequest(1, publicKey)
        cloneRequestSearch = SignedRequest(cloneSearchRequest, publicKey)
        requestSearchQuery = SignedRequest("search query", publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `create search request by query string`() {
        this.mvc.perform(post("/$version/client/$publicKey/search/request/1/query/")
                .content(requestSearchQuery.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `create search request`() {
        this.mvc.perform(post("/$version/client/$publicKey/search/request/")
                .content(requestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `create search request without createdAt and updatedAt`() {
        val strModel = requestSearch.toJsonString()
        val modelWithoutUpdateAt = strModel
                .replace(",\"updatedAt\":\"2019-02-19T10:35:56.503+0300\"", "")
                .replace(",\"createdAt\":\"2019-02-19T10:35:56.503+0300\"", "")

        this.mvc.perform(post("/$version/client/$publicKey/search/request/")
                .content(modelWithoutUpdateAt)
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `update search request`() {
        this.mvc.perform(post("/$version/client/$publicKey/search/request/1/")
                .content(cloneRequestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `delete search request`() {
        this.mvc.perform(delete("/$version/client/$publicKey/search/request/1/")
                .content(requestSearchId.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get search request by owner`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/request/")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get search request by owner and id`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/request/1/")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `clone search request`() {
        this.mvc.perform(put("/$version/client/$publicKey/search/request/")
                .content(cloneRequestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `get search requests by page`() {
        this.mvc.perform(get("/$version/search/requests?page=0&size=2")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get the total count of search requests`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/request/count")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get search request by owner and tag`() {
        this.mvc.perform(get("/$version/client/$publicKey/search/request/tag/car")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
