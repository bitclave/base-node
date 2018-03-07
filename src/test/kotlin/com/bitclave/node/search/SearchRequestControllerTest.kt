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

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class SearchRequestControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var requestSearch: SignedRequest<SearchRequest>
    protected lateinit var requestSearchId: SignedRequest<Long>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val searchRequest = SearchRequest(
            0,
            publicKey,
            mapOf("car" to "true", "color" to "red")
    )

    @Before fun setup() {
        requestSearch = SignedRequest(searchRequest, publicKey)
        requestSearchId = SignedRequest(1, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `create search request`() {
        this.mvc.perform(post("/client/$publicKey/search/")
                .content(requestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `delete search request`() {
        this.mvc.perform(delete("/client/$publicKey/search/1/")
                .content(requestSearchId.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get search request by owner`() {
        this.mvc.perform(get("/client/$publicKey/search/")
                .content(requestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get search request by owner and id`() {
        this.mvc.perform(get("/client/$publicKey/search/1/")
                .content(requestSearch.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
