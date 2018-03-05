package com.bitclave.node.requestData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.request.HybridRequestDataRepositoryImpl
import com.bitclave.node.repository.request.PostgresRequestDataRepositoryImpl
import com.bitclave.node.repository.request.RequestDataCrudRepository
import com.bitclave.node.repository.request.RequestDataRepositoryStrategy
import com.bitclave.node.services.RequestDataService
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

    protected val from = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    protected val to = "12710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

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

    @Test fun `get request by state and from`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                from,
                null,
                RequestData.RequestDataState.AWAIT,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request, RequestData.RequestDataState.AWAIT)
    }

    @Test fun `get request by state and to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                null,
                to,
                RequestData.RequestDataState.AWAIT,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request, RequestData.RequestDataState.AWAIT)
    }

    @Test fun `get request by state and from and to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByStatus(
                from,
                to,
                RequestData.RequestDataState.AWAIT,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request, RequestData.RequestDataState.AWAIT)
    }

    @Test fun `test response data after response to request ACCEPT`() {
        `create response to client with accept`()
        val resultRequests = requestDataService.getRequestByStatus(
                from,
                to,
                RequestData.RequestDataState.ACCEPT,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request, RequestData.RequestDataState.ACCEPT, RESPONSE_DATA)
    }

    @Test fun `test response data after response to request REJECT`() {
        `create response to client with reject`()

        val resultRequests = requestDataService.getRequestByStatus(
                from,
                to,
                RequestData.RequestDataState.REJECT,
                strategy
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request, RequestData.RequestDataState.REJECT, "")
    }

    protected open fun assertRequestData(
            request: RequestData,
            state: RequestData.RequestDataState,
            responseData: String = ""
    ) {
        Assertions.assertThat(request.fromPk).isEqualTo(from)
        Assertions.assertThat(request.toPk).isEqualTo(to)
        Assertions.assertThat(request.id).isEqualTo(1)
        Assertions.assertThat(request.requestData).isEqualTo(REQUEST_DATA)
        Assertions.assertThat(request.responseData).isEqualTo(responseData)
        Assertions.assertThat(request.state).isEqualTo(state)
    }

    @Test fun `create request to client`() {
        val id = requestDataService.request(from, request, strategy).get()
        Assertions.assertThat(id).isEqualTo(1)
    }

    @Test fun `create response to client with accept`() {
        `create request to client`()
        val state = requestDataService.response(1, to, RESPONSE_DATA, strategy).get()
        Assertions.assertThat(state).isEqualTo(RequestData.RequestDataState.ACCEPT)
    }

    @Test fun `create response to client with reject`() {
        `create request to client`()
        val state = requestDataService.response(1, to, null, strategy).get()
        Assertions.assertThat(state).isEqualTo(RequestData.RequestDataState.REJECT)
    }

}
