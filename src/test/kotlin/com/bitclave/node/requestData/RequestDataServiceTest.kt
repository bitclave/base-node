package com.bitclave.node.requestData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.entities.RequestData
import com.bitclave.node.repository.request.HybridRequestDataRepositoryImpl
import com.bitclave.node.repository.request.PostgresRequestDataRepositoryImpl
import com.bitclave.node.repository.request.RequestDataCrudRepository
import com.bitclave.node.repository.request.RequestDataRepositoryStrategy
import com.bitclave.node.services.errors.BadArgumentException
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

    companion object {
        protected const val REQUEST_DATA: String = "request_some_field"
        protected const val RESPONSE_DATA: String = "response_some_field"
    }

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var requestDataCrudRepository: RequestDataCrudRepository
    protected lateinit var requestDataService: RequestDataService

    protected val alisa = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"
    protected val bob = "023ea422076488339515e88a4110b9c6784d5cb1c0fa6a5a111b799a0e9b6aa720"
    protected val joe = "0399fdf3667119f99fd1da5eccba817d19c73dbdcbe92e678b4e37d18ccda2f178"

    protected lateinit var strategy: RepositoryStrategyType

    @Before
    fun setup() {
        val postgres = PostgresRequestDataRepositoryImpl(requestDataCrudRepository)
        val hybrid = HybridRequestDataRepositoryImpl(web3Provider, hybridProperties)

        val repositoryStrategy = RequestDataRepositoryStrategy(postgres, hybrid)
        requestDataService = RequestDataService(repositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
    }

    @Test
    fun `get request by from`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    @Test
    fun `get request by to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByParams(
            strategy,
            toPk = bob
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    @Test
    fun `get request by from and to`() {
        `create request to client`()

        val resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    protected fun assertRequestData(
        request: RequestData,
        responseData: String = ""
    ) {
        Assertions.assertThat(request.fromPk).isEqualTo(alisa)
        Assertions.assertThat(request.toPk).isEqualTo(bob)
        Assertions.assertThat(request.id).isEqualTo(1)
        Assertions.assertThat(request.requestData).isEqualTo(REQUEST_DATA)
        Assertions.assertThat(request.responseData).isEqualTo(responseData)
    }

    @Test
    fun `create request to client`() {
        val request = RequestData(
            0,
            alisa,
            bob,
            "some_base_id",
            REQUEST_DATA,
            RESPONSE_DATA
        )

        requestDataService.request(alisa, arrayListOf(request), strategy).get()
        val result = requestDataService.getRequestByParams(strategy, request.fromPk, request.toPk).get()
        Assertions.assertThat(result.size).isEqualTo(1)
        Assertions.assertThat(result[0].fromPk).isEqualTo(request.fromPk)
        Assertions.assertThat(result[0].toPk).isEqualTo(request.toPk)
        Assertions.assertThat(result[0].requestData).isEqualTo(request.requestData)
        Assertions.assertThat(result[0].rootPk).isEqualTo(bob)
        Assertions.assertThat(result[0].responseData).isEmpty()
    }

    @Test
    fun `create few same requests to client`() {
        `create request to client`()
        `create request to client`()
        `create request to client`()

        val resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()

        Assertions.assertThat(resultRequests.size).isEqualTo(1)
        val request = resultRequests[0]
        assertRequestData(request)
    }

    @Test
    fun `grant access to Bob from Alisa without request permissions`() {
        val grantRequest = RequestData(0, bob, alisa, alisa, "name_key", "Alisa")

        requestDataService.grantAccess(alisa, arrayListOf(grantRequest), strategy).get()

        val resultRequests = requestDataService.getRequestByParams(
            strategy,
            bob,
            alisa
        ).get()

        assert(resultRequests.size == 1)
        val resultRequest = resultRequests[0]

        Assertions.assertThat(resultRequest.fromPk).isEqualTo(bob)
        Assertions.assertThat(resultRequest.toPk).isEqualTo(alisa)
        Assertions.assertThat(resultRequest.id).isEqualTo(1)
        Assertions.assertThat(resultRequest.rootPk).isEqualTo(alisa)
        Assertions.assertThat(resultRequest.requestData).isEqualTo("name_key")
        Assertions.assertThat(resultRequest.responseData).isEqualTo("Alisa")
    }

    @Test
    fun `grant access for Bob form Alisa with request permissions from Bob`() {
        val requests = listOf(
            RequestData(
                0,
                bob,
                alisa,
                "some_base_id",
                "name_key"
            ),
            RequestData(
                0,
                bob,
                alisa,
                "some_base_id",
                "last_name_key"
            )
        )

        requestDataService.request(bob, requests, strategy).get()

        assert(
            requestDataService.getRequestByParams(
                strategy,
                bob,
                alisa
            ).get().size == 2
        )

        val grantRequests = arrayListOf(
            RequestData(0, bob, alisa, alisa, "name_key", "Alisa"),
            RequestData(0, bob, alisa, alisa, "some_another_key", "another_key_value")
        )

        requestDataService.grantAccess(alisa, grantRequests, strategy).get()

        var resultRequests = requestDataService.getRequestByParams(
            strategy,
            bob,
            alisa
        ).get()

        Assertions.assertThat(requestDataCrudRepository.findAll().toList().size == 2)

        assert(resultRequests.size == 3)

        resultRequests.forEach {
            Assertions.assertThat(it.fromPk).isEqualTo(bob)
            Assertions.assertThat(it.toPk).isEqualTo(alisa)
            Assertions.assertThat(it.rootPk).isEqualTo(alisa)
            when {
                it.requestData == "name_key" -> Assertions.assertThat(it.responseData).isEqualTo("Alisa")

                it.requestData == "some_another_key" ->
                    Assertions.assertThat(it.responseData).isEqualTo("another_key_value")

                it.requestData == "last_name_key" -> Assertions.assertThat(it.responseData).isEmpty()

                else -> throw IllegalStateException("undefined requestData value")
            }
        }

        resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()
        assert(resultRequests.isEmpty())
    }

    @Test
    fun `grant access for Bob form Alisa with request permissions from Bob and reShare data to Joe`() {
        val requests = listOf(
            RequestData(
                0,
                bob,
                alisa,
                "some_base_id",
                "name_key"
            ),
            RequestData(
                0,
                bob,
                alisa,
                "some_base_id",
                "last_name_key"
            )
        )

        requestDataService.request(bob, requests, strategy).get()

        assert(
            requestDataService.getRequestByParams(
                strategy,
                bob,
                alisa
            ).get().size == 2
        )

        val grantRequests = arrayListOf(
            RequestData(0, bob, alisa, alisa, "name_key", "Alisa"),
            RequestData(0, bob, alisa, alisa, "some_another_key", "another_key_value")
        )

        requestDataService.grantAccess(alisa, grantRequests, strategy).get()

        var resultRequests = requestDataService.getRequestByParams(
            strategy,
            bob,
            alisa
        ).get()

        Assertions.assertThat(requestDataCrudRepository.findAll().toList().size == 2)

        assert(resultRequests.size == 3)

        resultRequests.forEach {
            Assertions.assertThat(it.fromPk).isEqualTo(bob)
            Assertions.assertThat(it.toPk).isEqualTo(alisa)
            Assertions.assertThat(it.rootPk).isEqualTo(alisa)
            when {
                it.requestData == "name_key" -> Assertions.assertThat(it.responseData).isEqualTo("Alisa")

                it.requestData == "some_another_key" ->
                    Assertions.assertThat(it.responseData).isEqualTo("another_key_value")

                it.requestData == "last_name_key" -> Assertions.assertThat(it.responseData).isEmpty()

                else -> throw IllegalStateException("undefined requestData value")
            }
        }

        resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()
        assert(resultRequests.isEmpty())

        val grantForJoe = RequestData(0, joe, bob, alisa, "name_key", "Alisa")

        requestDataService.grantAccess(bob, arrayListOf(grantForJoe), strategy).get()

        val joeGrantedRequest = requestDataService.getRequestByParams(strategy, joe, bob).get()

        assert(joeGrantedRequest.size == 1)
        assert(joeGrantedRequest[0].rootPk == alisa)
        assert(joeGrantedRequest[0].toPk == bob)
        assert(joeGrantedRequest[0].fromPk == joe)
        assert(joeGrantedRequest[0].requestData == "name_key")
        assert(joeGrantedRequest[0].responseData == "Alisa")
    }

    @Test(expected = BadArgumentException::class)
    fun `grant access for Bob form Alisa with request permissions from Bob and try reShare not granted data to Joe`() {
        val requests = listOf(
            RequestData(
                0,
                bob,
                alisa,
                "some_base_id",
                "name_key"
            )
        )

        requestDataService.request(bob, requests, strategy).get()

        assert(
            requestDataService.getRequestByParams(
                strategy,
                bob,
                alisa
            ).get().size == 1
        )

        val grantRequests = arrayListOf(
            RequestData(0, bob, alisa, alisa, "name_key", "Alisa"),
            RequestData(0, bob, alisa, alisa, "some_another_key", "another_key_value")
        )

        requestDataService.grantAccess(alisa, grantRequests, strategy).get()

        var resultRequests = requestDataService.getRequestByParams(
            strategy,
            bob,
            alisa
        ).get()

        Assertions.assertThat(requestDataCrudRepository.findAll().toList().size == 2)

        assert(resultRequests.size == 2)

        resultRequests.forEach {
            Assertions.assertThat(it.fromPk).isEqualTo(bob)
            Assertions.assertThat(it.toPk).isEqualTo(alisa)
            Assertions.assertThat(it.rootPk).isEqualTo(alisa)
            when {
                it.requestData == "name_key" -> Assertions.assertThat(it.responseData).isEqualTo("Alisa")

                it.requestData == "some_another_key" ->
                    Assertions.assertThat(it.responseData).isEqualTo("another_key_value")

                else -> throw IllegalStateException("undefined requestData value")
            }
        }

        resultRequests = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()
        assert(resultRequests.isEmpty())

        val grantForJoe = RequestData(0, joe, bob, alisa, "last_name_key", "some data")

        try {
            requestDataService.grantAccess(bob, arrayListOf(grantForJoe), strategy).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }

    @Test
    fun `delete response and requests by From and To`() {
        val request = RequestData(
            0,
            bob,
            alisa,
            "some_base_id",
            "name_key"
        )

        requestDataService.request(alisa, listOf(request), strategy).get()

        val requestTo = RequestData(0, "", alisa, "", REQUEST_DATA)
        requestDataService.request(bob, listOf(requestTo), strategy).get()

        requestDataService.deleteRequestsAndResponses(alisa, strategy).get()

        var resultList = requestDataService.getRequestByParams(
            strategy,
            alisa,
            bob
        ).get()

        Assertions.assertThat(resultList.size).isEqualTo(0)

        resultList = requestDataService.getRequestByParams(
            strategy,
            bob,
            alisa
        ).get()
        Assertions.assertThat(resultList.size).isEqualTo(0)
    }

    @Test
    fun `grant access to Bob from Alisa reShare Alis data and revoke permissions`() {
        val manOne = "0327b531eaa68b3f302cb7e34c082b8d9e4b9ad1ea171002e73aa610efb5071258"
        val manTwo = "03de7be6442bba78bdf48ffeb2050bd7b1203c107a9f2a19776076ed0096ef461b"

        // Alisa grant for Bob
        val grantRequests = arrayListOf(
            RequestData(0, bob, alisa, alisa, "name_key", "Alisa"),
            RequestData(0, bob, alisa, alisa, "last_name_key", "is last name Alisa")
        )

        requestDataService.grantAccess(alisa, grantRequests, strategy).get()

        // Alisa grant for manTwo
        val grantRequestsAlisaManTwo =
            arrayListOf(RequestData(0, manTwo, alisa, alisa, "last_name_key", "is last name Alisa"))

        requestDataService.grantAccess(alisa, grantRequestsAlisaManTwo, strategy).get()

        // Bob reShare for Joe
        val grantForJoe = arrayListOf(
            RequestData(0, joe, bob, alisa, "name_key", "Alisa"),
            RequestData(0, joe, bob, alisa, "last_name_key", "is last name Alisa")
        )
        requestDataService.grantAccess(bob, grantForJoe, strategy).get()

        // Joe reShare for manOne and manTwo
        val grantForManOne = RequestData(0, manOne, joe, alisa, "name_key", "Alisa")
        requestDataService.grantAccess(joe, arrayListOf(grantForManOne), strategy).get()

        val grantForManTwo = RequestData(0, manTwo, joe, alisa, "name_key", "Alisa")
        requestDataService.grantAccess(joe, arrayListOf(grantForManTwo), strategy).get()

        var allItems = requestDataCrudRepository.findAll().toList()
        assert(allItems.size == 7)

        val revokeRequest = RequestData(0, bob, alisa, alisa, "name_key")
        requestDataService.revokeAccess(alisa, listOf(revokeRequest), strategy).get()

        val grantedFromAlisaToManTwo = requestDataCrudRepository.findByFromPkAndToPk(manTwo, alisa)
        assert(grantedFromAlisaToManTwo.size == 1)
        assert(grantedFromAlisaToManTwo[0].requestData == "last_name_key")

        allItems = requestDataCrudRepository.findAll().toList()
        assert(allItems.size == 3)

        allItems.forEach { assert(it.requestData == "last_name_key") }
    }
}
