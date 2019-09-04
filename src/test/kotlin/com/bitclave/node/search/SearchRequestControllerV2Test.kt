package com.bitclave.node.search

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.entities.SearchRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class SearchRequestControllerV2Test {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var cloneSearchRequest: SignedRequest<List<Long>>
    protected lateinit var updatedSearchRequests: SignedRequest<List<SearchRequest>>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        version = "v2"

        cloneSearchRequest = SignedRequest(listOf(1L), publicKey)
        updatedSearchRequests =
            SignedRequest(listOf(SearchRequest(1, publicKey, emptyMap())), publicKey)
        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `update search requests`() {
        this.mvc.perform(
            post("/$version/client/$publicKey/search/request")
                .content(updatedSearchRequests.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `clone search request`() {
        this.mvc.perform(
            put("/$version/client/$publicKey/search/request")
                .content(cloneSearchRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }
}
