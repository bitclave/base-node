package com.bitclave.node.file

import com.bitclave.node.extensions.toJsonString
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SignedRequest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected lateinit var version: String
    protected lateinit var requestFileId: SignedRequest<Long>
    protected lateinit var requestPk: SignedRequest<String>
    protected lateinit var testFile: MockMultipartFile
    private var httpHeaders: HttpHeaders = HttpHeaders()

    @Before
    fun setup() {
        version = "v1"
        requestPk = SignedRequest(publicKey, publicKey)
        requestFileId = SignedRequest(1, publicKey)

        testFile = MockMultipartFile("data", "filename.txt", "text/plain", "some xml".toByteArray())

        httpHeaders.set("Accept", "application/json")
        httpHeaders.set("Content-Type", "application/json")
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun `upload new file`() {
        this.mvc.perform(
            MockMvcRequestBuilders.fileUpload("/$version/file/$publicKey/")
                .file(testFile)
                .param("signature", requestPk.toJsonString())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `update existing file`() {
        this.mvc.perform(
            MockMvcRequestBuilders.fileUpload("/$version/file/$publicKey/1/")
                .file(testFile)
                .param("signature", requestPk.toJsonString())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `delete file`() {
        this.mvc.perform(
            delete("/$version/file/$publicKey/1/")
                .content(requestFileId.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `get file by owner and id`() {
        this.mvc.perform(
            get("/$version/file/$publicKey/1/")
                .content(requestFileId.toJsonString())
                .headers(httpHeaders)
        )
            .andExpect(status().isOk)
    }
}
