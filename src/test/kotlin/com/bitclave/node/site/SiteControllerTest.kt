package com.bitclave.node.site

import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.repository.models.Site
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class SiteControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val origin = "www.mysite.com"

    protected lateinit var siteRequest: SignedRequest<Site>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val site = Site(
            0,
            origin,
            publicKey
    )

    @Before fun setup() {
        version = "v1"

        siteRequest = SignedRequest(site, publicKey)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test fun `save information of site`() {
//        this.mvc.perform(post("/$version/site/")
//                .content(siteRequest.toJsonString())
//                .headers(httpHeaders))
//                .andExpect(status().isOk)
    }

    @Test fun `get information by origin`() {
        this.mvc.perform(get("/$version/site/$origin/")
                .headers(httpHeaders))
                .andExpect(status().isOk)
    }

}
