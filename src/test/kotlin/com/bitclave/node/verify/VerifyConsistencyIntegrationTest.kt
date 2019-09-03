package com.bitclave.node.verify

import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.models.SignedRequest
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VerifyConsistencyIntegrationTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val publicKey2 = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"
    protected lateinit var publicKeysRequest: SignedRequest<List<String>>
    private var httpHeaders: HttpHeaders = HttpHeaders()

    private val publicKeys = mutableListOf(publicKey, publicKey2)

    @Before
    fun setup() {

        publicKeysRequest = SignedRequest(publicKeys, publicKey)

        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.accept = mutableListOf(MediaType.APPLICATION_JSON)
        httpHeaders.set("Strategy", RepositoryStrategyType.POSTGRES.name)
    }

    @Test
    fun testHelloVerifyConsistencyController() {
        val requestEnty = HttpEntity<SignedRequest<List<String>>>(publicKeysRequest, httpHeaders)
        val result = testRestTemplate.postForEntity("/dev/verify/account/publickeys", requestEnty, Object::class.java)
        assertNotNull(result)
        assertEquals(result.statusCode, HttpStatus.FORBIDDEN)
    }
}
