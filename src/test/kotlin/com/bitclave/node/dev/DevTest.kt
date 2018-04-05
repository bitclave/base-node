package com.bitclave.node.dev

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import org.assertj.core.api.Assertions
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class BotControllerTest {


    @Autowired
    private lateinit var mvc: MockMvc

    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Test fun `check Adams name`() {
        val result: MvcResult = this.mvc.perform(get("/dev/bot/adam")
                .headers(httpHeaders))
                .andExpect(status().isOk)
                .andReturn()
        Assertions.assertThat(result.asyncResult).isEqualTo("038d4a758b58137ee47993ca434c1b797096536ada167b942f7d251ed1fc50c1c1");

    }
}
