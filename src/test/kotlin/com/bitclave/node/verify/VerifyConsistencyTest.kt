package com.bitclave.node.verify

import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.SignedRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
class VerifyConsistencyTest {

    @Autowired
    private lateinit var mvc: MockMvc

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var offerSearchRequest: SignedRequest<OfferSearch>
    protected lateinit var offerSearchIdRequest: SignedRequest<Long>
    protected lateinit var offerEventRequest: SignedRequest<String>
    protected lateinit var requestSearch: SignedRequest<SearchRequest>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

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

    private val searchRequest = SearchRequest(
            0,
            publicKey,
            mapOf("car" to "true", "color" to "red")
    )

    @Before fun setup() {

        offerSearchRequest = SignedRequest(offerSearchModel, publicKey)
        offerSearchIdRequest = SignedRequest(1L, publicKey)
        offerEventRequest = SignedRequest("bla-bla-bla", publicKey)

        requestSearch = SignedRequest(searchRequest, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `get offer search list by ids`() {
        this.mvc.perform(get("/dev/verify/offersearch/ids")
                .content(GSON.toJson(mutableListOf(1L, 2L, 3L, 4L)))
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }
}
