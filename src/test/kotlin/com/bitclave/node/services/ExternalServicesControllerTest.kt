package com.bitclave.node.services

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.models.SignedRequest
import com.bitclave.node.models.services.HttpServiceCall
import com.bitclave.node.models.services.ServiceCallType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class ExternalServicesControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var externalCallRequest: SignedRequest<HttpServiceCall>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        version = "v1"

        val headers = HttpHeaders()
        headers.set(HttpHeaders.ACCEPT, "application/json")
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json")
        headers.set("Strategy", RepositoryStrategyType.POSTGRES.name)

        externalCallRequest = SignedRequest(
            HttpServiceCall(
                publicKey,
                ServiceCallType.HTTP,
                HttpMethod.GET,
                "/v1/",
                hashMapOf("key" to "value"),
                headers
            ), publicKey
        )

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `should call external service with Http service call`() {
        this.mvc.perform(
            post("/$version/services/")
                .content(externalCallRequest.toJsonString())
                .headers(httpHeaders)
        ).andExpect(status().isOk)
    }

    @Test
    fun `should call get external service`() {
        this.mvc.perform(
            get("/$version/services/")
                .headers(httpHeaders)
        ).andExpect(status().isOk)
    }
}
