package com.bitclave.node.dev

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class RootControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc
    @Value("\${app.build.version}")
    private val version: String = "0.0.0"

    @Test
    fun `check health-check api`() {
        this.mvc.perform(get("/health-check"))
            .andExpect(status().isOk)
    }

    @Test
    fun `check version api`() {
        var result = this.mvc.perform(get("/ver"))
            .andExpect(status().isOk)
            .andReturn()

        Assertions.assertThat(result.asyncResult)
            .isEqualTo(version)

        result = this.mvc.perform(get("/version"))
            .andExpect(status().isOk)
            .andReturn()

        Assertions.assertThat(result.asyncResult)
            .isEqualTo(version)
    }
}
