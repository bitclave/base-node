package com.bitclave.node.share

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferShareData
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
class OfferShareControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var shareDataRequest: SignedRequest<OfferShareData>
    protected lateinit var worthRequest: SignedRequest<BigDecimal>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before fun setup() {
        version = "v1"

        shareDataRequest = SignedRequest(OfferShareData(
                1,
                publicKey,
                "",
                BigDecimal.ONE.toString()
        ), publicKey)
        worthRequest = SignedRequest(BigDecimal.TEN, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `create share data`() {
        this.mvc.perform(post("/$version/data/grant/offer/")
                .content(shareDataRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isCreated)
    }

    @Test fun `accept shared data`() {
        this.mvc.perform(patch("/$version/data/offer/")
                .param("offerSearchId", "1")
                .content(worthRequest.toJsonString())
                .headers(httpHeaders))
                .andExpect(status().isAccepted)
    }

    @Test fun `get share data by owner`() {
        this.mvc.perform(get("/$version/data/offer/")
                .param("owner", publicKey)
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

    @Test fun `get share data by owner and accepted`() {
        this.mvc.perform(get("/$version/data/offer/")
                .param("owner", publicKey)
                .param("accepted", "true")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
