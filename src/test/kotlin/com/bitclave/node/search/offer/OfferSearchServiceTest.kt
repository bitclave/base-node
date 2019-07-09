package com.bitclave.node.search.offer

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferInteraction
import com.bitclave.node.repository.models.OfferPrice
import com.bitclave.node.repository.models.OfferPriceRules
import com.bitclave.node.repository.models.OfferRank
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.offer.OfferCrudRepository
import com.bitclave.node.repository.offer.OfferRepositoryStrategy
import com.bitclave.node.repository.offer.PostgresOfferRepositoryImpl
import com.bitclave.node.repository.price.OfferPriceCrudRepository
import com.bitclave.node.repository.price.OfferPriceRepositoryStrategy
import com.bitclave.node.repository.price.PostgresOfferPriceRepositoryImpl
import com.bitclave.node.repository.priceRule.OfferPriceRulesCrudRepository
import com.bitclave.node.repository.rank.OfferRankCrudRepository
import com.bitclave.node.repository.rank.OfferRankRepositoryStrategy
import com.bitclave.node.repository.rank.PostgresOfferRankRepositoryImpl
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
import com.bitclave.node.repository.share.OfferShareCrudRepository
import com.bitclave.node.repository.share.OfferShareRepositoryStrategy
import com.bitclave.node.repository.share.PostgresOfferShareRepositoryImpl
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.OfferService
import com.bitclave.node.services.v1.OfferShareService
import com.bitclave.node.services.v1.SearchRequestService
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture
import java.util.stream.LongStream

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OfferSearchServiceTest {

    @Autowired
    private lateinit var gson: Gson

    @Autowired
    private lateinit var web3Provider: Web3Provider

    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository
    protected lateinit var offerService: OfferService

    @Autowired
    protected lateinit var offerRankCrudRepository: OfferRankCrudRepository

    @Autowired
    protected lateinit var offerPriceCrudRepository: OfferPriceCrudRepository

    @Autowired
    protected lateinit var offerPriceRuleCrudRepository: OfferPriceRulesCrudRepository

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository
    protected lateinit var searchRequestService: SearchRequestService

    @Autowired
    protected lateinit var offerSearchCrudRepository: OfferSearchCrudRepository
    protected lateinit var offerSearchService: OfferSearchService

    @Autowired
    protected lateinit var offerInteractionCrudRepository: OfferInteractionCrudRepository

    @Autowired
    protected lateinit var offerShareCrudRepository: OfferShareCrudRepository
    protected lateinit var offerShareService: OfferShareService

    @Autowired
    protected lateinit var querySearchRequestCrudRepository: QuerySearchRequestCrudRepository

    protected val rtSearchRepository = mock(RtSearchRepositoryImpl::class.java)

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val businessPublicKey = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType
    protected lateinit var createdOffer1: Offer
    protected lateinit var createdOffer2: Offer

    protected lateinit var createdSearchRequest1: SearchRequest
    protected lateinit var createdSearchRequest2: SearchRequest

    protected lateinit var rankForOffer1: OfferRank
    protected lateinit var rankForOffer2: OfferRank

    protected val offer = Offer(
        0,
        businessPublicKey,
        listOf(),
        "desc",
        "title",
        "url"
    )

    protected val offer2 = Offer(
        0,
        businessPublicKey,
        listOf(),
        "desc",
        "title",
        "url"
    )

    protected val offerPrice = OfferPrice(
        0,
        "first price description",
        BigDecimal("0.5").toString(),
        listOf(
            OfferPriceRules(0, "age", "10"),
            OfferPriceRules(0, "sex", "male"),
            OfferPriceRules(0, "country", "USA")
        )
    )

    protected lateinit var offerPrices: List<OfferPrice>
    private val searchPageRequest: PageRequest = PageRequest(0, 20)
    private lateinit var searchRequestRepositoryStrategy: SearchRequestRepositoryStrategy

    @Before
    fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)

        val offerShareRepository = PostgresOfferShareRepositoryImpl(offerShareCrudRepository)
        val shareRepositoryStrategy = OfferShareRepositoryStrategy(offerShareRepository)

        val searchRequestRepository =
            PostgresSearchRequestRepositoryImpl(
                searchRequestCrudRepository,
                offerSearchCrudRepository
            )
        searchRequestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)

        val offerSearchRepository = PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        val offerSearchStateRepository = PostgresOfferInteractionRepositoryImpl(offerInteractionCrudRepository)
        val offerSearchStateRepositoryStrategy = OfferInteractionRepositoryStrategy(offerSearchStateRepository)

        val offerPriceRepository =
            PostgresOfferPriceRepositoryImpl(offerPriceCrudRepository, offerPriceRuleCrudRepository)
        val offerPriceRepositoryStrategy = OfferPriceRepositoryStrategy(offerPriceRepository)

        val offerRankRepository = PostgresOfferRankRepositoryImpl(offerRankCrudRepository)
        val offerRankRepositoryStrategy = OfferRankRepositoryStrategy(offerRankRepository)

        offerShareService = OfferShareService(
            shareRepositoryStrategy,
            offerRepositoryStrategy,
            offerSearchRepositoryStrategy,
            searchRequestRepositoryStrategy,
            offerSearchStateRepositoryStrategy
        )

        offerSearchService = OfferSearchService(
            searchRequestRepositoryStrategy,
            offerRepositoryStrategy,
            offerSearchRepositoryStrategy,
            querySearchRequestCrudRepository,
            rtSearchRepository,
            offerSearchStateRepositoryStrategy,
            gson
        )

        offerService = OfferService(
            offerRepositoryStrategy,
            offerPriceRepositoryStrategy,
            offerRankRepositoryStrategy,
            offerSearchService
        )

        searchRequestService = SearchRequestService(
            searchRequestRepositoryStrategy,
            offerSearchRepositoryStrategy,
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

        rankForOffer1 = offerRankRepositoryStrategy
            .changeStrategy(strategy)
            .saveRankOffer(OfferRank(0, 11, createdOffer1.id, publicKey))

        rankForOffer2 = offerRankRepositoryStrategy
            .changeStrategy(strategy)
            .saveRankOffer(OfferRank(0, 1, createdOffer2.id, publicKey))

        for (i in 0 until 5) {
            offerRepositoryStrategy
                .changeStrategy(strategy)
                .saveOffer(offer)
        }

        offerPrices = offerPriceRepositoryStrategy
            .changeStrategy(strategy)
            .savePrices(offer, listOf(offerPrice))

        createdSearchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, emptyMap()))

        createdSearchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, emptyMap()))
    }

    fun createOfferSearch(searchRequest: SearchRequest, offer: Offer) {
        offerSearchService.saveNewOfferSearch(
            OfferSearch(
                0,
                searchRequest.owner,
                searchRequest.id,
                offer.id
            ),
            strategy
        ).get()
    }

    @Test
    fun `should be create QuerySearchRequest`() {
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

        val queryRequestsByOwner = querySearchRequestCrudRepository
            .findAllByOwner(publicKey)
        val existedSearchRequest = searchRequestCrudRepository.findOne(searchRequestWithRtSearch.id)

        assertThat(existedSearchRequest)
        assertThat(queryRequestsByOwner.size == 1)
        assertThat(queryRequestsByOwner[0].query).isEqualTo(searchQueryText)
        assertThat(offersResult.size == list.size)
    }

    @Test
    fun `should be delete QuerySearchRequest by owner`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        var existedSearchRequest = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assertThat(existedSearchRequest.size == 1)

        offerSearchService.deleteByOwner(publicKey, strategy).get()

        existedSearchRequest = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assertThat(existedSearchRequest.isEmpty())
    }

    @Test
    fun `should be create offersSearch items`() {
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

        val searchResult = offerSearchCrudRepository
            .findBySearchRequestId(searchRequestWithRtSearch.id)
        assert(searchResult.size == 3)
        assert(searchResult.filter { list.indexOf(it.offerId) > -1 }.size == 3)
        assertThat(offersResult.size == list.size)
    }

    @Test
    fun `should be delete all old offersSearch items by query`() {
        val searchRequestWithRtSearch = searchRequestService.putSearchRequest(
            0,
            publicKey,
            SearchRequest(0, publicKey, mapOf("rtSearch" to "true")),
            strategy
        ).get()

        val firstList: Page<Long> = PageImpl(arrayListOf<Long>(1, 2, 3, 4, 5), searchPageRequest, 1)
        Mockito.`when`(rtSearchRepository.getOffersIdByQuery("some data", searchPageRequest))
            .thenReturn(CompletableFuture.completedFuture(firstList))

        offerSearchService.createOfferSearchesByQuery(
            searchRequestWithRtSearch.id, publicKey, "some data", searchPageRequest, strategy
        ).get()

        val secondList: Page<Long> = PageImpl(arrayListOf<Long>(4, 5), searchPageRequest, 1)
        Mockito.`when`(rtSearchRepository.getOffersIdByQuery("some data", searchPageRequest))
            .thenReturn(CompletableFuture.completedFuture(secondList))
        offerSearchService.createOfferSearchesByQuery(
            searchRequestWithRtSearch.id, publicKey, "some data", searchPageRequest, strategy
        ).get()

        val searchResult = offerSearchCrudRepository.findByOwner(publicKey)
        assert(searchResult.size == 2)
        assert(searchResult.filter { secondList.indexOf(it.offerId) > -1 }.size == 2)
    }

    @Test(expected = NotFoundException::class)
    fun `should be throw by unknown searchRequestId`() {
        try {
            offerSearchService.createOfferSearchesByQuery(
                1234343, publicKey, "some data", searchPageRequest, strategy
            ).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }

    @Test(expected = BadArgumentException::class)
    fun `should be throw by searchRequestId not has rtSearch tag`() {
        val searchRequestWithoutRtSearch = searchRequestService.putSearchRequest(
            0,
            publicKey,
            SearchRequest(0, publicKey, emptyMap()),
            strategy
        ).get()
        try {
            offerSearchService.createOfferSearchesByQuery(
                searchRequestWithoutRtSearch.id, publicKey, "some data", searchPageRequest, strategy
            ).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }

    @Test
    fun `should be create new offer search item and get result by clientId and search request id`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id)
            .get()
            .content

        val state = offerInteractionCrudRepository
            .findByOfferIdAndOwner(createdOffer1.id, createdSearchRequest1.owner)

        assert(result.isNotEmpty())
        assert(result[0].offerSearch.id >= 1L)
        assert(state!!.state == OfferAction.NONE)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `should be create multiple offer search items and get result by owner`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        val result = offerSearchService.getOffersAndOfferSearchesByParams(strategy, publicKey)
            .get()
            .content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)
    }

    @Test
    fun `should be valid page size and count of items`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        var result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(0, 2)
        )
            .get()
            .content

        assert(result.size == 2)
        assert(result[0].offerSearch.id == 1L)
        assert(result[1].offerSearch.id == 2L)

        result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(1, 2)
        )
            .get()
            .content

        assert(result.size == 2)
        assert(result[0].offerSearch.id == 3L)
        assert(result[1].offerSearch.id == 4L)

        result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(0, 20)
        )
            .get()
            .content

        assert(result.size == 4)
        assert(result[0].offerSearch.id == 1L)
        assert(result[1].offerSearch.id == 2L)
        assert(result[2].offerSearch.id == 3L)
        assert(result[3].offerSearch.id == 4L)

        result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(5, 20)
        )
            .get()
            .content

        assert(result.size == 0)
    }

    @Test
    fun `should be return zero by group and owner`() {
        val searchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_clothing,_accessories" to "true")))

        val searchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_bitclave_general" to "true")))

        createOfferSearch(searchRequest1, createdOffer2)
        createOfferSearch(searchRequest1, createdOffer1)
        createOfferSearch(searchRequest2, createdOffer2)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                unique = false,
                searchRequestIds = arrayListOf(12345L)
            ).get()
            .content

        assert(result.size == 0)
    }

    @Test
    fun `should be return by group and owner`() {
        val searchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_clothing,_accessories" to "true")))

        val searchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_bitclave_general" to "true")))

        createOfferSearch(searchRequest1, createdOffer2)
        createOfferSearch(searchRequest1, createdOffer1)
        createOfferSearch(searchRequest2, createdOffer2)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                unique = false,
                searchRequestIds = arrayListOf(searchRequest1.id)
            ).get()
            .content

        assert(result.size == 2)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
    }

    @Test
    fun `should be return by state and owner`() {
        val searchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_clothing,_accessories" to "true")))

        val searchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, businessPublicKey, mapOf("interest_bitclave_general" to "true")))

        createOfferSearch(searchRequest1, createdOffer2)
        createOfferSearch(searchRequest1, createdOffer1)
        createOfferSearch(searchRequest2, createdOffer2)

        val allOffersByOwner = offerSearchService
            .getOffersAndOfferSearchesByParams(strategy, publicKey)
            .get()
            .content

        assertThat(allOffersByOwner.size == 2)

        val complainTo = allOffersByOwner[0].offerSearch.id
        offerSearchService.complain(complainTo, publicKey, strategy).get()
        offerSearchService.reject(allOffersByOwner[1].offerSearch.id, publicKey, strategy).get()

        var result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                unique = false,
                searchRequestIds = emptyList(),
                state = arrayListOf(OfferAction.COMPLAIN)
            ).get()
            .content

        assert(result.size == 1)
        assert(result[0].offerSearch.id == complainTo)

        result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                businessPublicKey,
                unique = false,
                searchRequestIds = emptyList(),
                state = arrayListOf(OfferAction.NONE)
            ).get()
            .content

        assert(result.size == 1)
        assert(result[0].offerSearch.offerId == createdOffer2.id)
    }

    @Test
    fun `should be return by unique (by offerId) and owner`() {
        val searchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_clothing,_accessories" to "true")))

        val searchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
            .save(SearchRequest(0, publicKey, mapOf("interest_bitclave_general" to "true")))

        createOfferSearch(searchRequest1, createdOffer2)
        createOfferSearch(searchRequest1, createdOffer1)
        createOfferSearch(searchRequest2, createdOffer2)

        var result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                unique = true,
                searchRequestIds = arrayListOf(searchRequest1.id)
            ).get()
            .content

        assert(result.size == 2)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)

        result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                unique = true
            ).get()
            .content

        assert(result.size == 2)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
    }

    @Test
    fun `should be get by multiple ids`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        val result = offerSearchService.getOfferSearchesByIds(strategy, mutableListOf(1L, 2L, 3L, 4L)).get()
        assert(result.size == 4)
    }

    @Test
    fun `should be add EVENT as serialized object into array`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)

        offerSearchService.addEventTo("bla bla bla", createdSearchRequest1.id, strategy).get()

        val state = offerInteractionCrudRepository.findByOfferIdAndOwner(createdOffer1.id, createdSearchRequest1.owner)

        assert(state!!.events.contains("bla bla bla"))
        assert(state.updatedAt.time > state.createdAt.time)
    }

    @Test
    fun `should be create new offer search item and get result by clientId and offer search id`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)

        val result = offerSearchService.getOffersResult(strategy, 0, createdSearchRequest1.id)
            .get()
            .content

        assert(result.size == 1)
        assert(result[0].offerSearch.id == createdSearchRequest1.id)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `client can complain to search item`() {
        `should be create new offer search item and get result by clientId and search request id`()

        offerSearchService.complain(1L, publicKey, strategy).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id)
            .get()
            .content

        val state = offerInteractionCrudRepository.findByOfferIdAndOwner(1L, createdSearchRequest1.owner)

        assert(result.isNotEmpty())
        assert(result[0].offerSearch.id >= 1L)
        assert(state!!.state == OfferAction.COMPLAIN)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `search item state should be ACCEPT`() {
        `should be create new offer search item and get result by clientId and search request id`()

        val projectId = offerPrices[0].id
        val offerShareData =
            OfferShareData(1L, businessPublicKey, publicKey, "response", BigDecimal.ZERO.toString(), true, projectId)

        offerShareService.grantAccess(
            publicKey,
            offerShareData,
            strategy
        ).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id)
            .get()
            .content

        val state =
            offerInteractionCrudRepository.findByOfferIdAndOwner(result[0].offer.id, createdSearchRequest1.owner)
        assert(result.size == 1)
        assert(result[0].offerSearch.id == createdSearchRequest1.id)
        assert(state!!.state == OfferAction.ACCEPT)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `all search item states with same owner and offerId should be same when one of them is updated`() {
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        `client can complain to search item`()

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        val state1 = offerInteractionCrudRepository.findByOfferIdAndOwner(createdOffer1.id, createdSearchRequest2.owner)

        assert(result.size == 2)
        assert(result[0].id >= 1L)
        assert(state1!!.state == OfferAction.COMPLAIN)
        assert(result[1].id >= 1L)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id, createdSearchRequest1.id).get()
        val state2 = offerInteractionCrudRepository.findByOfferIdAndOwner(createdOffer2.id, createdSearchRequest1.owner)

        assert(result.size == 1)
        assert(result[0].id >= 1L)
        assert(state2!!.state == OfferAction.NONE)
    }

    @Test
    fun `a new offerSearch item should be sync with related offerSearch items if exists`() {
        `client can complain to search item`()

        createOfferSearch(createdSearchRequest2, createdOffer1)

        val result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        val state = offerInteractionCrudRepository.findByOfferIdAndOwner(createdOffer1.id, createdSearchRequest2.owner)

        assert(result.size == 2)
        assert(result[0].id >= 1L)
        assert(state!!.state == OfferAction.COMPLAIN)
        assert(result[1].id >= 1L)
    }

    @Test
    fun `delete all OfferSearch objects with state NONE or REJECT when related Offer object is updated`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        val changedOffer = Offer(
            createdOffer1.id,
            createdOffer1.owner,
            listOf(),
            "is desc111",
            "is title111",
            "is image url111",
            BigDecimal.ONE.toString(),
            mapOf("color" to "red"),
            mapOf("salary" to "1000"),
            mapOf("salary" to Offer.CompareAction.MORE)
        )

        offerService.putOffer(createdOffer1.id, createdOffer1.owner, changedOffer, strategy).get()

        result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.isEmpty())

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isNotEmpty())
    }

    @Test
    fun `delete all OfferSearch objects when related Offer object is deleted`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        offerService.deleteOffer(createdOffer1.id, createdOffer1.owner, strategy).get()

        result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.isEmpty())

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isNotEmpty())
    }

    @Test
    fun `delete all OfferSearch objects when related SearchRequest object is deleted`() {
        `client can complain to search item`()
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isNotEmpty())

        searchRequestService.deleteSearchRequest(createdSearchRequest1.id, createdSearchRequest1.owner, strategy).get()

        result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 1)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isEmpty())
    }

    @Test
    fun `get all dangling OfferSearch objects by SearchRequest`() {
        `delete all OfferSearch objects when related SearchRequest object is deleted`()

        val result = offerSearchService.getDanglingOfferSearches(strategy, false, true).get()
        assert(result.isEmpty())
    }

    @Test
    fun `get all dangling OfferSearch objects by Offer`() {
        `delete all OfferSearch objects when related Offer object is deleted`()

        val result = offerSearchService.getDanglingOfferSearches(strategy, true, false).get()
        assert(result.isEmpty())
    }

    @Test
    fun `get offerSearches with the same owner and offerId but different content`() {
        `a new offerSearch item should be sync with related offerSearch items if exists`()

        val result = offerSearchService.getDiffOfferSearches(strategy).get()
        assert(result.isEmpty())
    }

    @Test
    fun `get total count of offerSearches`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        val result = offerSearchService.getOfferSearchTotalCount(strategy).get()
        assert(result == 3L)
    }

    @Test
    fun `get total count of offerSearches by searchRequestIds`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)

        val ids = arrayListOf(createdSearchRequest1.id, createdSearchRequest1.id, createdSearchRequest2.id, 123L)
        val result = offerSearchService
            .getOfferSearchCountBySearchRequestIds(ids, strategy)
            .get()

        assertThat(result[createdSearchRequest1.id] == 2L)
        assertThat(result[createdSearchRequest2.id] == 1L)
        assertThat(result[123] == 0L)
    }

    @Test
    fun `should return all offerSearch results by page`() {
        LongStream.range(0, 4).forEach {
            val offer = Offer(
                0,
                businessPublicKey,
                listOf(),
                "desc",
                "title",
                "url"
            )

            offerCrudRepository.save(offer)

            val request = SearchRequest(0, publicKey, emptyMap())
            searchRequestCrudRepository.save(request)

            offerSearchService.saveNewOfferSearch(
                OfferSearch(0, request.owner, request.id, offer.id),
                strategy
            ).get()
        }

        val firstPage = offerSearchService.getPageableOfferSearches(PageRequest(0, 2), strategy).get()
        assertThat(firstPage.size).isEqualTo(2)
        assert(firstPage.first().id == 1L)
        assert(firstPage.last().id == 2L)

        val secondPage = offerSearchService.getPageableOfferSearches(PageRequest(1, 2), strategy).get()
        assertThat(secondPage.size).isEqualTo(2)
        assert(secondPage.first().id == 3L)
        assert(secondPage.last().id == 4L)
    }

    @Test
    fun `clone all OfferSearch objects when from search request to search request`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        offerSearchService.cloneOfferSearchOfSearchRequest(createdSearchRequest1.id, createdSearchRequest2, strategy)
            .get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest2.id)
            .get()
            .content

        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `should return offers & offerSearches by owner with default sorting`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        val result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(0, 4)
        ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        assert(result[0].offer.id == createdOffer1.id)
        assert(result[1].offer.id == createdOffer2.id)
        assert(result[2].offer.id == createdOffer1.id)
        assert(result[3].offer.id == createdOffer2.id)
    }

    @Test
    fun `should return offers & offerSearches by owner with rank sorting`() {
        createOfferSearch(createdSearchRequest1, createdOffer1)
        createOfferSearch(createdSearchRequest1, createdOffer2)
        createOfferSearch(createdSearchRequest2, createdOffer1)
        createOfferSearch(createdSearchRequest2, createdOffer2)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                emptyList(),
                PageRequest(0, 4, Sort("rank"))
            ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        assert(result[0].offer.id == createdOffer1.id) // rank 11
        assert(result[1].offer.id == createdOffer1.id)
        assert(result[2].offer.id == createdOffer2.id) // rank 1
        assert(result[3].offer.id == createdOffer2.id)
    }

    @Test
    fun `should return offers & offerSearches by owner with sorting by updatedAt`() {

        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest2, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val result = offerSearchService.getOffersAndOfferSearchesByParams(
            strategy, publicKey, false, emptyList(), emptyList(), PageRequest(0, 4, Sort("updatedAt"))
        ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        assert(result[0].offer.id == savedOffer3.id)
        assert(result[1].offer.id == savedOffer2.id)
        assert(result[2].offer.id == savedOffer1.id)
        assert(result[3].offer.id == savedOffer1.id)
    }

    @Test
    fun `should return offers & offerSearches by owner and state with default sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest2, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val offerSearches = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                emptyList(),
                PageRequest(0, 4)
            ).get().content

        offerSearches.forEach {
            offerSearchService.complain(it.offerSearch.id, publicKey, strategy).get()
        }

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                arrayListOf(OfferAction.COMPLAIN)
            ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        val existedIds = mutableListOf(savedOffer1.id, savedOffer1.id, savedOffer2.id, savedOffer3.id)

        result.forEach {
            val pos = existedIds.indexOf(it.offer.id)
            assert(pos > -1)
            existedIds.removeAt(pos)
        }

        assert(existedIds.isEmpty())
    }

    @Test
    fun `should return offers & offerSearches by owner and state with rank sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offerRankRepository = PostgresOfferRankRepositoryImpl(offerRankCrudRepository)
        val offerRankRepositoryStrategy = OfferRankRepositoryStrategy(offerRankRepository)
        val rankRepository = offerRankRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        rankRepository.saveRankOffer(OfferRank(0, 1, savedOffer2.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 2, savedOffer3.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 3, savedOffer1.id, publicKey))

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest2, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val offerSearches = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                emptyList(),
                PageRequest(0, 4)
            ).get().content

        offerSearches.forEach {
            offerSearchService.complain(it.offerSearch.id, publicKey, strategy).get()
        }

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                arrayListOf(OfferAction.COMPLAIN),
                PageRequest(0, 4, Sort("rank"))
            ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        assert(result[0].offer.id == savedOffer1.id)
        assert(result[1].offer.id == savedOffer1.id)
        assert(result[2].offer.id == savedOffer3.id)
        assert(result[3].offer.id == savedOffer2.id)
    }

    @Test
    fun `should return offers & offerSearches and OfferIntracations`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offerRankRepository = PostgresOfferRankRepositoryImpl(offerRankCrudRepository)
        val offerRankRepositoryStrategy = OfferRankRepositoryStrategy(offerRankRepository)
        val rankRepository = offerRankRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        rankRepository.saveRankOffer(OfferRank(0, 1, savedOffer2.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 2, savedOffer3.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 3, savedOffer1.id, publicKey))

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest2, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val offerSearches = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                emptyList(),
                PageRequest(0, 4)
            ).get().content

        offerSearches.forEach {
            offerSearchService.complain(it.offerSearch.id, publicKey, strategy).get()
        }

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                arrayListOf(OfferAction.COMPLAIN),
                PageRequest(0, 4, Sort("rank")),
                true
            ).get().content

        result.forEach {
            assert(it.interaction != null)
            assert(it.interaction!!.offerId == it.offer.id)
            assert(it.interaction!!.owner == it.offerSearch.owner)
        }
    }

    @Test
    fun `should return offers & offerSearches by owner and state with updated time sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest2, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val offerSearches = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                emptyList(),
                PageRequest(0, 4)
            ).get().content

        offerSearches.forEach {
            offerSearchService.complain(it.offerSearch.id, publicKey, strategy).get()
        }

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                emptyList(),
                arrayListOf(OfferAction.COMPLAIN),
                PageRequest(0, 4, Sort("updatedAt"))
            ).get().content

        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)

        assert(result[0].offer.id == savedOffer1.id)
        assert(result[1].offer.id == savedOffer1.id)
        assert(result[2].offer.id == savedOffer3.id)
        assert(result[3].offer.id == savedOffer2.id)
    }

    @Test
    fun `should return offers & offerSearches by owner and searchRequestIds with default sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest1, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                arrayListOf(createdSearchRequest1.id),
                emptyList(),
                PageRequest(0, 4)
            ).get().content

        assert(result.size == 3)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)

        assert(result[0].offer.id == savedOffer1.id)
        assert(result[1].offer.id == savedOffer2.id)
        assert(result[2].offer.id == savedOffer3.id)
    }

    @Test
    fun `should return offers & offerSearches by owner and searchRequestIds with rank sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offerRankRepository = PostgresOfferRankRepositoryImpl(offerRankCrudRepository)
        val offerRankRepositoryStrategy = OfferRankRepositoryStrategy(offerRankRepository)
        val rankRepository = offerRankRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        rankRepository.saveRankOffer(OfferRank(0, 1, savedOffer2.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 2, savedOffer3.id, publicKey))
        rankRepository.saveRankOffer(OfferRank(0, 3, savedOffer1.id, publicKey))

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest1, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                arrayListOf(createdSearchRequest1.id),
                emptyList(),
                PageRequest(0, 4, Sort("rank"))
            ).get().content

        assert(result.size == 3)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)

        assert(result[0].offer.id == savedOffer1.id)
        assert(result[1].offer.id == savedOffer3.id)
        assert(result[2].offer.id == savedOffer2.id)
    }

    @Test
    fun `should return offers & offerSearches by owner and searchRequestIds with updatedAt sorting`() {
        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)
        val repository = offerRepositoryStrategy.changeStrategy(strategy)

        val offer1 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer2 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")
        Thread.sleep(1000)
        val offer3 = Offer(0, businessPublicKey, listOf(), "desc", "title", "url")

        val savedOffer1 = repository.saveOffer(offer1)
        val savedOffer2 = repository.saveOffer(offer2)
        val savedOffer3 = repository.saveOffer(offer3)

        createOfferSearch(createdSearchRequest1, savedOffer1)
        createOfferSearch(createdSearchRequest1, savedOffer2)
        createOfferSearch(createdSearchRequest1, savedOffer3)
        createOfferSearch(createdSearchRequest2, savedOffer1)

        val result = offerSearchService
            .getOffersAndOfferSearchesByParams(
                strategy,
                publicKey,
                false,
                arrayListOf(createdSearchRequest1.id),
                emptyList(),
                PageRequest(0, 4, Sort("updatedAt"))
            ).get().content

        assert(result.size == 3)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)

        assert(result[0].offer.id == savedOffer3.id)
        assert(result[1].offer.id == savedOffer2.id)
        assert(result[2].offer.id == savedOffer1.id)
    }

    @Test
    fun `should return interactions by owner`() {
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 123))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 1234))

        val result = offerSearchService.getInteractions(publicKey).get()
        assert(result.size == 1)
        assert(result[0].owner == publicKey)
        assert(result[0].offerId == 123L)
    }

    @Test
    fun `should return interactions by owner and states`() {
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 5, OfferAction.EVALUATE))

        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 5, OfferAction.EVALUATE))

        val result = offerSearchService
            .getInteractions(publicKey, listOf(OfferAction.COMPLAIN, OfferAction.REJECT))
            .get()

        assert(result.size == 3)
        assert(result[0].owner == publicKey)
        assert(result[0].offerId == 2L)
        assert(result[0].state == OfferAction.COMPLAIN)

        assert(result[1].owner == publicKey)
        assert(result[1].offerId == 3L)
        assert(result[1].state == OfferAction.COMPLAIN)

        assert(result[2].owner == publicKey)
        assert(result[2].offerId == 4L)
        assert(result[2].state == OfferAction.REJECT)
    }

    @Test
    fun `should return interactions by owner and offers`() {
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 5, OfferAction.EVALUATE))

        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 5, OfferAction.EVALUATE))

        val result = offerSearchService
            .getInteractions(publicKey, emptyList(), listOf(1, 2, 7))
            .get()

        assert(result.size == 2)
        assert(result[0].owner == publicKey)
        assert(result[0].offerId == 1L)
        assert(result[0].state == OfferAction.NONE)

        assert(result[1].owner == publicKey)
        assert(result[1].offerId == 2L)
        assert(result[1].state == OfferAction.COMPLAIN)
    }

    @Test
    fun `should return interactions by owner, states and offers`() {
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, publicKey, 5, OfferAction.EVALUATE))

        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 1))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 2, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 3, OfferAction.COMPLAIN))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 4, OfferAction.REJECT))
        offerInteractionCrudRepository.save(OfferInteraction(0, businessPublicKey, 5, OfferAction.EVALUATE))

        val result = offerSearchService
            .getInteractions(publicKey, listOf(OfferAction.COMPLAIN), listOf(1, 3, 7))
            .get()

        assert(result.size == 1)
        assert(result[0].owner == publicKey)
        assert(result[0].offerId == 3L)
        assert(result[0].state == OfferAction.COMPLAIN)
    }
}
