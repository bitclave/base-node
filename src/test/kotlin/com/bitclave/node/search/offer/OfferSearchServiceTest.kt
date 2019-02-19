package com.bitclave.node.search.offer

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.*
import com.bitclave.node.repository.offer.OfferCrudRepository
import com.bitclave.node.repository.offer.OfferRepositoryStrategy
import com.bitclave.node.repository.offer.PostgresOfferRepositoryImpl
import com.bitclave.node.repository.price.OfferPriceCrudRepository
import com.bitclave.node.repository.price.OfferPriceRepositoryStrategy
import com.bitclave.node.repository.price.PostgresOfferPriceRepositoryImpl
import com.bitclave.node.repository.priceRule.OfferPriceRulesCrudRepository
import com.bitclave.node.repository.search.PostgresSearchRequestRepositoryImpl
import com.bitclave.node.repository.search.SearchRequestCrudRepository
import com.bitclave.node.repository.search.SearchRequestRepositoryStrategy
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepositoryStrategy
import com.bitclave.node.repository.search.offer.PostgresOfferSearchRepositoryImpl
import com.bitclave.node.repository.share.OfferShareCrudRepository
import com.bitclave.node.repository.share.OfferShareRepositoryStrategy
import com.bitclave.node.repository.share.PostgresOfferShareRepositoryImpl
import com.bitclave.node.services.v1.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.util.*
import java.util.stream.LongStream

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
open class OfferSearchServiceTest {

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
    protected lateinit var offerShareCrudRepository: OfferShareCrudRepository
    protected lateinit var offerShareService: OfferShareService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val businessPublicKey = "03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType
    protected lateinit var createdOffer1: Offer
    protected lateinit var createdOffer2: Offer

    protected lateinit var createdSearchRequest1: SearchRequest
    protected lateinit var createdSearchRequest2: SearchRequest

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

    protected val offerPrices = listOf(offerPrice)

    @Before fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)

        val offerShareRepository = PostgresOfferShareRepositoryImpl(offerShareCrudRepository)
        val shareRepositoryStrategy = OfferShareRepositoryStrategy(offerShareRepository)

        val searchRequestRepository = PostgresSearchRequestRepositoryImpl(searchRequestCrudRepository, offerSearchCrudRepository)
        val searchRequestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)

        val offerSearchRepository = PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository, searchRequestRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        val offerPriceRepository = PostgresOfferPriceRepositoryImpl(offerPriceCrudRepository, offerPriceRuleCrudRepository)
        val offerPriceRepositoryStrategy = OfferPriceRepositoryStrategy(offerPriceRepository)


        offerShareService = OfferShareService(
                shareRepositoryStrategy,
                offerRepositoryStrategy,
                offerSearchRepositoryStrategy,
                searchRequestRepositoryStrategy
        )

        offerSearchService = OfferSearchService(
                searchRequestRepositoryStrategy,
                offerRepositoryStrategy,
                offerSearchRepositoryStrategy
        )

        offerService = OfferService(
                offerRepositoryStrategy,
                offerPriceRepositoryStrategy
        )

        searchRequestService = SearchRequestService(
                searchRequestRepositoryStrategy
        )

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)


        createdOffer1 = offerRepositoryStrategy
                .changeStrategy(strategy)
                .saveOffer(offer)

        createdOffer2 = offerRepositoryStrategy
                .changeStrategy(strategy)
                .saveOffer(offer2)

        offerPriceRepositoryStrategy
                .changeStrategy(strategy)
                .savePrices(offer, offerPrices)


        createdSearchRequest1 = searchRequestRepositoryStrategy.changeStrategy(strategy)
                .saveSearchRequest(SearchRequest(0, publicKey, emptyMap()))

        createdSearchRequest2 = searchRequestRepositoryStrategy.changeStrategy(strategy)
                .saveSearchRequest(SearchRequest(0, publicKey, emptyMap()))
    }

    fun createOfferSearch(searchRequest: SearchRequest, offer: Offer, events: MutableList<String>) {
        offerSearchService.saveNewOfferSearch(
                OfferSearch(
                        0,
                        searchRequest.owner,
                        searchRequest.id,
                        offer.id,
                        OfferResultAction.NONE,
                        "",
                        events
                ),
                strategy
        ).get()
    }

    @Test
    fun `should be create new offer search item and get result by clientId and search request id`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id).get()
        assert(result.isNotEmpty())
        assert(result[0].offerSearch.id >= 1L)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `should be create multiple offer search items and get result by owner`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer2, ArrayList())

        val result = offerSearchService.getOffersAndOfferSearchesByOwnerResult(strategy, publicKey).get()
        assert(result.size == 4)
        assert(result[0].offerSearch.offerId == result[0].offer.id)
        assert(result[1].offerSearch.offerId == result[1].offer.id)
        assert(result[2].offerSearch.offerId == result[2].offer.id)
        assert(result[3].offerSearch.offerId == result[3].offer.id)
    }

    @Test
    fun `should be get by multiple ids`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer2, ArrayList())

        val result = offerSearchService.getOfferSearchesByIds(strategy, mutableListOf(1L, 2L, 3L, 4L)).get()
        assert(result.size == 4)
    }

    @Test
    fun `should be add EVENT as serialized object into array`() {
        var events = mutableListOf("tram taram")
        createOfferSearch(createdSearchRequest1, createdOffer1, events)

        offerSearchService.addEventTo("bla bla bla", 1L, strategy).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id).get()
        assert(result[0].offerSearch.events.contains("bla bla bla"))

    }

    @Test
    fun `should be create new offer search item and get result by clientId and offer search id`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())

        val result = offerSearchService.getOffersResult(strategy, null, createdSearchRequest1.id).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == createdSearchRequest1.id)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `client can complain to search item`() {
        `should be create new offer search item and get result by clientId and search request id`()

        offerSearchService.complain(1L, businessPublicKey, strategy).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id).get()
        assert(result.isNotEmpty())
        assert(result[0].offerSearch.id >= 1L)
        assert(result[0].offerSearch.state == OfferResultAction.COMPLAIN)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `search item state should be ACCEPT`() {
        `should be create new offer search item and get result by clientId and search request id`()

        val projectId = offerPrices[0].id
        val offerShareData = OfferShareData(1L, businessPublicKey, publicKey, "response", BigDecimal.ZERO.toString(), true, projectId)

        offerShareService.grantAccess(
                publicKey,
                offerShareData,
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest1.id).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == createdSearchRequest1.id)
        assert(result[0].offerSearch.state == OfferResultAction.ACCEPT)
        assert(result[0].offer.id == createdOffer1.id)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `all search item states with same owner and offerId should be same when one of them is updated`() {
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())

        `client can complain to search item`()

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)
        assert(result[0].id >= 1L)
        assert(result[0].state == OfferResultAction.COMPLAIN)
        assert(result[1].id >= 1L)
        assert(result[1].state == OfferResultAction.COMPLAIN)
        assertThat(result[0].events.toList()).isEqualTo(result[1].events.toList())
        assert(result[0].updatedAt == result[1].updatedAt)
        assert(result[0].info == result[1].info)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id, createdSearchRequest1.id).get()
        assert(result.size == 1)
        assert(result[0].id >= 1L)
        assert(result[0].state == OfferResultAction.NONE)
    }

    @Test fun `a new offerSearch item should be sync with related offerSearch items if exists`() {
        `client can complain to search item`()

        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())

        val result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)
        assert(result[0].id >= 1L)
        assert(result[0].state == OfferResultAction.COMPLAIN)
        assert(result[1].id >= 1L)
        assert(result[1].state == OfferResultAction.COMPLAIN)
        assert(result[1].state == OfferResultAction.COMPLAIN)
        assertThat(result[0].events.toList()).isEqualTo(result[1].events.toList())
        assert(result[0].updatedAt == result[1].updatedAt)
        assert(result[0].info == result[1].info)
    }

    @Test fun `delete all OfferSearch objects with state NONE or REJECT when related Offer object is updated`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())

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

    @Test fun `delete all OfferSearch objects when related Offer object is deleted`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        offerService.deleteOffer(createdOffer1.id, createdOffer1.owner, strategy).get()

        result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.isEmpty())

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isNotEmpty())
    }

    @Test fun `delete all OfferSearch objects with state NONE or REJECT when related SearchRequest object is deleted`() {
        `client can complain to search item`()
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())

        var result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isNotEmpty())

        searchRequestService.deleteSearchRequest(createdSearchRequest1.id, createdSearchRequest1.owner, strategy).get()

        result = offerSearchService.getOfferSearches(strategy, createdOffer1.id).get()
        assert(result.size == 2)

        result = offerSearchService.getOfferSearches(strategy, createdOffer2.id).get()
        assert(result.isEmpty())
    }

    @Test fun `get all dangling OfferSearch objects by SearchRequest`() {
        `delete all OfferSearch objects with state NONE or REJECT when related SearchRequest object is deleted`()

        var result = offerSearchService.getDanglingOfferSearches(strategy, false, true).get()
        assert(result.size == 1)
        assert(result[0].searchRequestId == createdSearchRequest1.id)
    }

    @Test fun `get all dangling OfferSearch objects by Offer`() {
        `delete all OfferSearch objects when related Offer object is deleted`()

        var result = offerSearchService.getDanglingOfferSearches(strategy, true, false).get()
        assert(result.isEmpty())
    }

    @Test fun `get offerSearches with the same owner and offerId but different content`() {
        `a new offerSearch item should be sync with related offerSearch items if exists`()

        var result = offerSearchService.getDiffOfferSearches(strategy).get()
        assert(result.isEmpty())
    }

    @Test fun `get total count of offerSearches`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())

        var result = offerSearchService.getOfferSearchTotalCount(strategy).get()
        assert(result == 3L)
    }

    @Test fun `should return all offersearch results by page`() {
        LongStream.range(0, 4).forEach { id ->
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
                    OfferSearch(0, request.owner, request.id, offer.id, OfferResultAction.NONE),
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

    @Test fun `clone all OfferSearch objects when from search request to search request`() {
        createOfferSearch(createdSearchRequest1, createdOffer1, ArrayList())
        createOfferSearch(createdSearchRequest1, createdOffer2, ArrayList())
        createOfferSearch(createdSearchRequest2, createdOffer2, ArrayList())

        offerSearchService.cloneOfferSearchOfSearchRequest(createdSearchRequest1.id, createdSearchRequest2, strategy).get()

        val result = offerSearchService.getOffersResult(strategy, createdSearchRequest2.id).get()
        assertThat(result.size).isEqualTo(2)
    }
}
