package com.bitclave.node.requestData

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.entities.RequestData
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class RequestDataControllerTest {

    protected val from = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected val to = "12710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    @Autowired
    private lateinit var mvc: MockMvc

    protected lateinit var version: String

    protected lateinit var requestDataRequest: SignedRequest<List<RequestData>>
    protected lateinit var requestDataResponse: SignedRequest<String>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        version = "v1"

        requestDataRequest = SignedRequest(listOf(RequestData()), from)
        requestDataResponse = SignedRequest("", from)

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `get request by state`() {
        this.mvc.perform(
            get("/$version/data/request/")
                .param("alisa", from)
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)

        this.mvc.perform(
            get("/$version/data/request/")
                .param("alisa", from)
                .param("bob", to)
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)

        this.mvc.perform(
            get("/$version/data/request/")
                .param("bob", to)
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `create request for client`() {
        this.mvc.perform(
            post("/$version/data/request/")
                .content(requestDataRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `grant access for client`() {
        this.mvc.perform(
            post("/$version/data/grant/request/")
                .content(requestDataRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `revoke access for client`() {
        this.mvc.perform(
            delete("/$version/data/grant/request/")
                .content(requestDataRequest.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }
}
