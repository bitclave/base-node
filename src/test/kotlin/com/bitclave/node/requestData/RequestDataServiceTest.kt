package com.bitclave.node.requestData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.request.HybridRequestDataRepositoryImpl
import com.bitclave.node.repository.request.PostgresRequestDataRepositoryImpl
import com.bitclave.node.repository.request.RequestDataCrudRepository
import com.bitclave.node.repository.request.RequestDataRepositoryStrategy
import com.bitclave.node.services.v1.RequestDataService
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RequestDataServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var requestDataCrudRepository: RequestDataCrudRepository
    protected lateinit var requestDataService: RequestDataService

    protected val from = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"
    protected val to = "023ea422076488339515e88a4110b9c6784d5cb1c0fa6a5a111b799a0e9b6aa720"

    protected val REQUEST_DATA: String = "REQUEST_DATA"
    protected val RESPONSE_DATA: String = "RESPONSE_DATA"
    protected lateinit var request: RequestData

    protected lateinit var strategy: RepositoryStrategyType

    @Before fun setup() {
        request = RequestData(1, "", to, REQUEST_DATA)

        val postgres = PostgresRequestDataRepositoryImpl(requestDataCrudRepository)
        val hybrid = HybridRequestDataRepositoryImpl(web3Provider, hybridProperties)

        val repositoryStrategy = RequestDataRepositoryStrategy(postgres, hybrid)
        requestDataService = RequestDataService(repositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
    }

    @Test fun `get request by from`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                from,
                null,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    @Test fun `get request by to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                null,
                to,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    @Test fun `get request by from and to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                from,
                to,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    protected open fun assertRequestData(
            request: RequestData,
            responseData: String = ""
    ) {
        Assertions.assertThat(request.fromPk).isEqualTo(from)
        Assertions.assertThat(request.toPk).isEqualTo(to)
        Assertions.assertThat(request.id).isEqualTo(1)
        Assertions.assertThat(request.requestData).isEqualTo(REQUEST_DATA)
        Assertions.assertThat(request.responseData).isEqualTo(responseData)
    }

    @Test fun `create request to client`() {
        val id = requestDataService.request(from, request, strategy).get()
        Assertions.assertThat(id).isEqualTo(1L)
    }

    @Test fun `grant access to client with accept`() {
        val grantRequest = RequestData(0, from, to, "", RESPONSE_DATA)
        val id = requestDataService.grantAccess(to, grantRequest, strategy).get()
        Assertions.assertThat(id).isEqualTo(1L)
        val resultRequests = requestDataService.getRequestByStatus(
                from,
                to,
                strategy
        ).get()

        assert(resultRequests.size == 1)
        val resultRequest = resultRequests[0]

        Assertions.assertThat(resultRequest.fromPk).isEqualTo(from)
        Assertions.assertThat(resultRequest.toPk).isEqualTo(to)
        Assertions.assertThat(resultRequest.id).isEqualTo(1)
        Assertions.assertThat(resultRequest.requestData).isEmpty()
        Assertions.assertThat(resultRequest.responseData).isEqualTo(RESPONSE_DATA)
    }

    @Test fun `delete response and requests by From and To`() {
        var id = requestDataService.request(from, request, strategy).get()
        Assertions.assertThat(id).isEqualTo(1L)

        val requestTo = RequestData(0, "", from, REQUEST_DATA)
        id = requestDataService.request(to, requestTo, strategy).get()
        Assertions.assertThat(id).isEqualTo(2L)

        requestDataService.deleteRequestsAndResponses(from, strategy).get()
        var resultList = requestDataService.getRequestByStatus(
                from,
                to,
                strategy
        ).get()
        Assertions.assertThat(resultList.size).isEqualTo(0)

        resultList = requestDataService.getRequestByStatus(
                to,
                from,
                strategy
        ).get()
        Assertions.assertThat(resultList.size).isEqualTo(0)
    }

}
