package com.bitclave.node.offer

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
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
import java.math.BigDecimal

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class OfferControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var requestOffer: SignedRequest<Offer>
    protected lateinit var requestOfferId: SignedRequest<Long>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val offer = Offer(
            0,
            publicKey,
            "is desc",
            "is title",
            "is image url",
            BigDecimal.TEN.toString(),
            mapOf("car" to "true", "color" to "red"),
            mapOf("age" to "18", "salary" to "1000"),
            mapOf("age" to Offer.CompareAction.MORE_OR_EQUAL, "salary" to Offer.CompareAction.MORE_OR_EQUAL))

    @Before fun setup() {
        version = "v1"
        requestOffer = SignedRequest<Offer>(offer, publicKey)
        requestOfferId = SignedRequest<Long>(1, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `create offer`() {
        this.mvc.perform(put("/$version/client/$publicKey/offer/")
                .content(requestOffer.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `update offer`() {
        this.mvc.perform(put("/$version/client/$publicKey/offer/1/")
                .content(requestOffer.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `delete offer`() {
        this.mvc.perform(delete("/$version/client/$publicKey/offer/1/")
                .content(requestOfferId.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offer by owner`() {
        this.mvc.perform(get("/$version/client/$publicKey/offer/")
                .content(requestOffer.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get offer by owner and id`() {
        this.mvc.perform(get("/$version/client/$publicKey/offer/1/")
                .content(requestOffer.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
