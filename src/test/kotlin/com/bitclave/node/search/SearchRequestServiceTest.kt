package com.bitclave.node.search

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.offer.OfferCrudRepository
import com.bitclave.node.repository.offer.OfferRepositoryStrategy
import com.bitclave.node.repository.offer.PostgresOfferRepositoryImpl
import com.bitclave.node.repository.rtSearch.RtSearchRepositoryImpl
import com.bitclave.node.repository.search.PostgresSearchRequestRepositoryImpl
import com.bitclave.node.repository.search.SearchRequestCrudRepository
import com.bitclave.node.repository.search.SearchRequestRepositoryStrategy
import com.bitclave.node.repository.search.interaction.OfferInteractionCrudRepository
import com.bitclave.node.repository.search.interaction.OfferInteractionRepositoryStrategy
import com.bitclave.node.repository.search.interaction.PostgresOfferInteractionRepositoryImpl
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepositoryStrategy
import com.bitclave.node.repository.search.offer.PostgresOfferSearchRepositoryImpl
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.SearchRequestService
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.CompletableFuture
import javax.persistence.EntityManager

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SearchRequestServiceTest {

    @Autowired
    private lateinit var gson: Gson

    @Autowired
    private lateinit var web3Provider: Web3Provider

    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository
    protected lateinit var searchRequestService: SearchRequestService

    @Autowired
    protected lateinit var offerSearchCrudRepository: OfferSearchCrudRepository
    protected lateinit var offerSearchService: OfferSearchService

    @Autowired
    protected lateinit var offerInteractionCrudRepository: OfferInteractionCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository

    @Autowired
    protected lateinit var querySearchRequestCrudRepository: QuerySearchRequestCrudRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    protected val rtSearchRepository = Mockito.mock(RtSearchRepositoryImpl::class.java)

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    protected lateinit var createdOffer1: Offer
    protected lateinit var createdOffer2: Offer

    private val searchRequest = SearchRequest(
        0,
        account.publicKey,
        mapOf("car" to "true", "color" to "red")
    )

    private val searchRequest2 = SearchRequest(
        0,
        account.publicKey,
        mapOf("car" to "true", "color" to "red")
    )

    private val searchRequest3 = SearchRequest(
        0,
        account.publicKey,
        mapOf("bike" to "true", "color" to "black")
    )

    protected val offer = Offer(
        0,
        account.publicKey,
        listOf(),
        "desc",
        "title",
        "url"
    )

    protected val offer2 = Offer(
        0,
        account.publicKey,
        listOf(),
        "desc",
        "title",
        "url"
    )

    private val ignoredFields = arrayOf("id", "createdAt", "updatedAt")

    @Before
    fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)
        val searchRequestRepository =
            PostgresSearchRequestRepositoryImpl(
                searchRequestCrudRepository,
                offerSearchCrudRepository,
                entityManager
            )
        val requestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository, entityManager)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)

        val offerSearchRepository =
            PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        val offerSearchStateRepository =
            PostgresOfferInteractionRepositoryImpl(offerInteractionCrudRepository, entityManager)
        val offerSearchStateRepositoryStrategy = OfferInteractionRepositoryStrategy(offerSearchStateRepository)

        offerSearchService = OfferSearchService(
            requestRepositoryStrategy,
            offerRepositoryStrategy,
            offerSearchRepositoryStrategy,
            querySearchRequestCrudRepository,
            rtSearchRepository,
            offerSearchStateRepositoryStrategy,
            gson
        )

        searchRequestService = SearchRequestService(
            requestRepositoryStrategy,
            querySearchRequestCrudRepository,
            offerSearchService
        )

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)

        createdOffer1 = offerRepositoryStrategy
            .changeStrategy(strategy)
            .saveOffer(offer)

        createdOffer2 = offerRepositoryStrategy
            .changeStrategy(strategy)
            .saveOffer(offer2)
    }

    @Test
    fun `should be create new search request`() {
        val result = searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest,
            strategy
        ).get()

        assert(result.id >= 1L)
        assertThat(result.owner).isEqualTo(account.publicKey)
        assertThat(result.tags).isEqualTo(searchRequest.tags)
        assertThat(result.createdAt.time > searchRequest.createdAt.time)
        assertThat(result.updatedAt.time > searchRequest.updatedAt.time)
    }

    @Test
    fun `should be create few new search requests`() {
        val result = searchRequestService.putSearchRequests(
            account.publicKey,
            listOf(searchRequest, searchRequest),
            strategy
        ).get()

        assert(result.size == 2)

        result.forEach { item ->
            assert(item.id >= 1L)
            assertThat(item.owner).isEqualTo(account.publicKey)
            assertThat(item.tags).isEqualTo(searchRequest.tags)
            assertThat(item.createdAt.time > searchRequest.createdAt.time)
            assertThat(item.updatedAt.time > searchRequest.updatedAt.time)
        }
    }

    @Test
    fun `should be update few new search requests`() {
        `should be create few new search requests`()

        val existed = searchRequestService.getPageableRequests(PageRequest(0, 1000), strategy).get()
        assert(existed.content.size == 2)
        val updatedTags = mapOf("test" to "true")
        val forUpdate = existed.map { it.copy(tags = updatedTags) }.content

        val result = searchRequestService.putSearchRequests(
            account.publicKey,
            forUpdate,
            strategy
        ).get()

        assert(result.size == 2)
        val originGrouped = existed.content.groupBy { it.id }

        result.forEach { item ->
            assert(originGrouped.contains(item.id))
            assertThat(item.owner).isEqualTo(account.publicKey)
            assertThat(item.tags).isEqualTo(updatedTags)
            assertThat(item.createdAt.time == originGrouped[item.id]?.get(0)?.createdAt?.time ?: -1)
            assertThat(item.updatedAt.time > originGrouped[item.id]?.get(0)?.updatedAt?.time ?: -1)
        }
    }

    @Test(expected = AccessDeniedException::class)
    fun `should be throw error when try update search requests with diff owner`() {
        val foreingSearchRequest = SearchRequest(
            0,
            account.publicKey.reversed(),
            mapOf("car" to "true", "color" to "red")
        )

        val mySR = searchRequestService.putSearchRequests(
            account.publicKey,
            listOf(searchRequest),
            strategy
        ).get()[0]

        val frSR = searchRequestService.putSearchRequests(
            account.publicKey.reversed(),
            listOf(foreingSearchRequest),
            strategy
        ).get()[0]

        try {
            searchRequestService.putSearchRequests(
                account.publicKey,
                listOf(mySR, frSR),
                strategy
            ).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }

    @Test
    fun `should delete existed search request by owner`() {
        val searchPageRequest = PageRequest(0, 20)

        val list: Page<Long> = PageImpl(arrayListOf<Long>(1, 2, 3), searchPageRequest, 1)

        val searchRequestWithRtSearch = searchRequestService.putSearchRequest(
            0,
            publicKey,
            SearchRequest(0, publicKey, mapOf("rtSearch" to "true")),
            strategy
        ).get()

        val searchQueryText = "some data"
        Mockito.`when`(rtSearchRepository.getOffersIdByQuery(searchQueryText, searchPageRequest))
            .thenReturn(CompletableFuture.completedFuture(list))

        val offersResult = offerSearchService.createOfferSearchesByQuery(
            searchRequestWithRtSearch.id, publicKey, searchQueryText, searchPageRequest, strategy
        ).get()

        var queryRequestsByOwner = querySearchRequestCrudRepository
            .findAllByOwner(publicKey)
        val existedSearchRequest = searchRequestCrudRepository.findOne(searchRequestWithRtSearch.id)

        assertThat(existedSearchRequest)
        assertThat(queryRequestsByOwner.size == 1)
        assertThat(queryRequestsByOwner[0].query).isEqualTo(searchQueryText)
        assertThat(offersResult.size == list.size)

        searchRequestService.deleteQuerySearchRequest(publicKey).get()

        queryRequestsByOwner = querySearchRequestCrudRepository
            .findAllByOwner(publicKey)
        assertThat(queryRequestsByOwner.isEmpty())
    }

    @Test
    fun `should update existed search request`() {
        `should be create new search request`()

        var savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        val existedRequest = savedListResult[0]

        val updateSearchRequest = existedRequest.copy(
            owner = account.publicKey,
            tags = mapOf("car" to "false", "color" to "blue")
        )

        val result = searchRequestService.putSearchRequest(1, account.publicKey, updateSearchRequest, strategy).get()

        assert(result.id >= 1L)
        assertThat(result.owner).isEqualTo(account.publicKey)
        assertThat(result.tags).isEqualTo(updateSearchRequest.tags)
        assertThat(result.createdAt).isEqualTo(updateSearchRequest.createdAt)
        assertThat(result.updatedAt.time > updateSearchRequest.updatedAt.time)

        savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
    }

    @Test
    fun `should delete existed search request`() {
        `should be create new search request`()

        var savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        val deletedId = searchRequestService.deleteSearchRequest(1, account.publicKey, strategy).get()

        assert(deletedId == 1L)

        savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test
    fun `should delete all existed search request`() {
        `should be create new search request`()
        `should be create new search request`()
        `should be create new search request`()

        var savedListResult = searchRequestService.getSearchRequests(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(3)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)
        assertThat(savedListResult[1]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)
        assertThat(savedListResult[2]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        searchRequestService.deleteSearchRequests(account.publicKey, strategy).get()

        savedListResult = searchRequestService.getSearchRequests(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test
    fun `should return search requests by id and owner`() {
        `should be create new search request`()
        `should be create new search request`()

        var result = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        result = searchRequestService.getSearchRequests(2, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        result = searchRequestService.getSearchRequests(3, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `should return search requests by owner`() {
        `should be create new search request`()
        `should be create new search request`()

        val result = searchRequestService.getSearchRequests(0, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(2)
        assert(result[0].id == 1L)
        assert(result[1].id == 2L)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)
        assertThat(result[1]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)
    }

    @Test
    fun `should clone existed search request with related offerSearches`() {
        val result1 = searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest,
            strategy
        ).get()

        val result2 = searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest2,
            strategy
        ).get()

        offerSearchService.saveNewOfferSearch(
            OfferSearch(
                0,
                result1.owner,
                result1.id,
                createdOffer1.id
            ),
            strategy
        ).get()

        offerSearchService.saveNewOfferSearch(
            OfferSearch(
                0,
                result1.owner,
                result1.id,
                createdOffer2.id
            ),
            strategy
        ).get()

        offerSearchService.complain(1L, createdOffer1.owner, strategy).get()

        offerSearchService.saveNewOfferSearch(
            OfferSearch(
                0,
                result2.owner,
                result2.id,
                createdOffer1.id
            ),
            strategy
        ).get()

        val clonedRequest = searchRequestService
            .cloneSearchRequestWithOfferSearches(account.publicKey, listOf(result1.id), strategy)
            .get()

        assertThat(clonedRequest.size == 1)
        assertThat(clonedRequest[0]).isEqualToIgnoringGivenFields(searchRequest, *ignoredFields)

        val savedListResult =
            searchRequestService.getSearchRequests(clonedRequest[0].id, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)

        val offerSearches = offerSearchService.getOffersResult(strategy, result1.id)
            .get()
            .content

        assertThat(offerSearches.size).isEqualTo(2)

        val clonedOfferSearches = offerSearchService.getOffersResult(strategy, clonedRequest[0].id)
            .get()
            .content

        assertThat(clonedOfferSearches.size).isEqualTo(2)
    }

    @Test
    fun `should return all search requests by page`() {
        `should be create new search request`()
        `should be create new search request`()
        `should be create new search request`()
        `should be create new search request`()

        val firstPage = searchRequestService.getPageableRequests(PageRequest(0, 2), strategy).get()
        assertThat(firstPage.size).isEqualTo(2)
        assert(firstPage.first().id == 1L)
        assert(firstPage.last().id == 2L)

        val secondPage = searchRequestService.getPageableRequests(PageRequest(1, 2), strategy).get()
        assertThat(secondPage.size).isEqualTo(2)
        assert(secondPage.first().id == 3L)
        assert(secondPage.last().id == 4L)
    }

    @Test
    fun `get total count of search requests`() {
        `should be create new search request`()
        `should be create new search request`()
        `should be create new search request`()
        `should be create new search request`()

        val result = searchRequestService.getSearchRequestTotalCount(strategy).get()
        assert(result == 4L)
    }

    @Test
    fun `should return search requests by owner and tag key`() {
        searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest,
            strategy
        ).get()

        searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest2,
            strategy
        ).get()

        searchRequestService.putSearchRequest(
            0,
            account.publicKey,
            searchRequest3,
            strategy
        ).get()

        var result = searchRequestService.getRequestByOwnerAndTag(account.publicKey, "color", strategy).get()
        assertThat(result.size).isEqualTo(3)

        result = searchRequestService.getRequestByOwnerAndTag(account.publicKey, "car", strategy).get()
        assertThat(result.size).isEqualTo(2)

        result = searchRequestService.getRequestByOwnerAndTag(account.publicKey, "notexist", strategy).get()
        assertThat(result.size).isEqualTo(0)
    }
}
