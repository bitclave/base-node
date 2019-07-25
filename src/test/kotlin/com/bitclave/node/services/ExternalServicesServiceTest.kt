package com.bitclave.node.services

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.services.ExternalService
import com.bitclave.node.repository.models.services.HttpServiceCall
import com.bitclave.node.repository.models.services.ServiceCallType
import com.bitclave.node.repository.services.ExternalServicesCrudRepository
import com.bitclave.node.repository.services.ExternalServicesRepositoryStrategy
import com.bitclave.node.repository.services.PostgresExternalServicesRepositoryImpl
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.services.CallStrategy
import com.bitclave.node.services.v1.services.CallStrategyImpl
import com.bitclave.node.services.v1.services.ExternalServicesService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate
import java.util.Date

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ExternalServicesServiceTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    @Autowired
    protected lateinit var servicesCrudRepository: ExternalServicesCrudRepository

    protected lateinit var callStrategy: CallStrategy

    protected lateinit var externalServicesService: ExternalServicesService

    @Autowired
    private lateinit var web3Provider: Web3Provider

    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val endpoint = "http://mysite.com/api/v1"
    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val externalService = ExternalService(
        account.publicKey,
        endpoint
    )

    @Before
    fun setup() {
        val accountPostgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val accountRepositoryStrategy = AccountRepositoryStrategy(accountPostgres, hybrid)

        val accountService = AccountService(accountRepositoryStrategy)

        val postgres = PostgresExternalServicesRepositoryImpl(servicesCrudRepository)
        val repositoryStrategy = ExternalServicesRepositoryStrategy(postgres)

        callStrategy = CallStrategyImpl(restTemplate)
        externalServicesService = ExternalServicesService(repositoryStrategy, callStrategy)

        strategy = RepositoryStrategyType.POSTGRES
        repositoryStrategy.changeStrategy(strategy).save(externalService)
        accountService.registrationClient(account, strategy)
    }

    @Test
    fun `should be return registered service`() {
        val result = externalServicesService.findAll(strategy).get()
        assert(result.isNotEmpty())
        assert(result[0].endpoint == endpoint)
        assert(result[0].publicKey == publicKey)
    }

    @Test
    fun `should be call simple GET request`() {

        val entity = HttpEntity<Any>(null, HttpHeaders())

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(HttpStatus.OK))

        val serviceCall = HttpServiceCall(publicKey, ServiceCallType.HTTP, HttpMethod.GET, "/")
        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.OK.value())
    }

    @Test
    fun `should be call simple GET request with NOT_FOUND`() {

        val entity = HttpEntity<Any>(null, HttpHeaders())

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(HttpStatus.NOT_FOUND))

        val serviceCall = HttpServiceCall(publicKey, ServiceCallType.HTTP, HttpMethod.GET, "/")
        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `should be call GET request with query args`() {

        val entity = HttpEntity<Any>(null, HttpHeaders())

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/?var1=1,2,3&var2=hello"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(mapOf("var1" to "1,2,3", "var2" to "hello"))
            )
        ).thenReturn(ResponseEntity<Any>(Account("0x0", 10), HttpStatus.OK))

        val serviceCall = HttpServiceCall(
            publicKey,
            ServiceCallType.HTTP,
            HttpMethod.GET,
            "/",
            mapOf("var1" to "1,2,3", "var2" to "hello")
        )

        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.OK.value())
        assert(result.body != null)
        assert((result.body as Account).publicKey == "0x0")
        assert((result.body as Account).nonce == 10L)
    }

    @Test
    fun `should be call GET request with headers`() {

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = arrayListOf(MediaType.APPLICATION_JSON)

        val entity = HttpEntity<Any>(null, headers)

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(headers, HttpStatus.ACCEPTED))

        val serviceCall = HttpServiceCall(
            publicKey,
            ServiceCallType.HTTP,
            HttpMethod.GET,
            "/",
            emptyMap(),
            headers
        )

        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.ACCEPTED.value())
        assertThat(result.headers).isEqualTo(headers)
    }

    @Test
    fun `should be call POST request with headers`() {

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = arrayListOf(MediaType.APPLICATION_JSON)

        val entity = HttpEntity<Any>(null, headers)

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(headers, HttpStatus.CREATED))

        val serviceCall = HttpServiceCall(
            publicKey,
            ServiceCallType.HTTP,
            HttpMethod.POST,
            "/",
            emptyMap(),
            headers
        )

        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.CREATED.value())
        assertThat(result.headers).isEqualTo(headers)
    }

    @Test
    fun `should be call POST request with headers and body`() {

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = arrayListOf(MediaType.APPLICATION_JSON)

        val account = Account("0x0", 10, Date(), Date())
        val entity = HttpEntity<Any>(account, headers)

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(headers, HttpStatus.CREATED))

        val serviceCall = HttpServiceCall(
            publicKey,
            ServiceCallType.HTTP,
            HttpMethod.POST,
            "/",
            emptyMap(),
            headers,
            account
        )

        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.CREATED.value())
        assertThat(result.headers).isEqualTo(headers)
    }

    @Test
    fun `should be call POST request with headers and body with FORBIDDEN`() {

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = arrayListOf(MediaType.APPLICATION_JSON)

        val account = Account("0x0", 10, Date(), Date())
        val entity = HttpEntity<Any>(account, headers)

        Mockito.`when`(
            restTemplate.exchange(
                ArgumentMatchers.eq("$endpoint/"),
                ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq(entity),
                ArgumentMatchers.eq(Any::class.java),
                ArgumentMatchers.eq(emptyMap<String, String>())
            )
        ).thenReturn(ResponseEntity<Any>(headers, HttpStatus.FORBIDDEN))

        val serviceCall = HttpServiceCall(
            publicKey,
            ServiceCallType.HTTP,
            HttpMethod.POST,
            "/",
            emptyMap(),
            headers,
            account
        )

        val result = externalServicesService.externalCall(serviceCall, strategy).get()
        assert(result.status == HttpStatus.FORBIDDEN.value())
        assertThat(result.headers).isEqualTo(headers)
    }
}
