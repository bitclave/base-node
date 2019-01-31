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
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.OfferShareService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OfferSearchServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository

    @Autowired
    protected lateinit var offerPriceCrudRepository: OfferPriceCrudRepository

    @Autowired
    protected lateinit var offerPriceRuleCrudRepository: OfferPriceRulesCrudRepository

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository

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

    protected val offer = Offer(
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
                    OfferPriceRules(0,"age","10"),
                    OfferPriceRules(0,"sex","male"),
                    OfferPriceRules(0,"country","USA")
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

        val searchRequestRepository = PostgresSearchRequestRepositoryImpl(searchRequestCrudRepository)
        val searchRequestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerRepository = PostgresOfferRepositoryImpl(offerCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(offerRepository)

        val offerSearchRepository = PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
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

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)



        offerRepositoryStrategy
                .changeStrategy(strategy)
                .saveOffer(offer)

        offerPriceRepositoryStrategy
                .changeStrategy(strategy)
                .savePrices(offer, offerPrices)

        searchRequestRepositoryStrategy.changeStrategy(strategy)
                .saveSearchRequest(SearchRequest(0, publicKey, emptyMap()))
    }

    @Test
    fun `should be create new offer search item and get result by clientId and search request id`() {
        offerSearchService.saveNewOfferSearch(
                OfferSearch(0, 1L, 1L, OfferResultAction.NONE, "","", ArrayList()),
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id >= 1L)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `should be add EVENT as serialized object into array`() {
        var events = mutableListOf("tram taram")
        offerSearchService.saveNewOfferSearch(
                OfferSearch(0, 1L, 1L, OfferResultAction.NONE, "", "", events),
                strategy
        ).get()

        offerSearchService.addEventTo( "bla bla bla",1L, strategy).get()


        val result = offerSearchService.getOffersResult(strategy, 1L).get()
        assert(result[0].offerSearch.events.contains("bla bla bla"))

    }

    @Test
    fun `should be create new offer search item and get result by clientId and offer search id`() {
        offerSearchService.saveNewOfferSearch(
                OfferSearch(0, 1L, 1L, OfferResultAction.NONE, "", "", ArrayList()),
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(strategy, null, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `client can complain to search item`() {
        `should be create new offer search item and get result by clientId and search request id`()

        offerSearchService.complain(1L, publicKey, strategy).get()

        val result = offerSearchService.getOffersResult(strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id >= 1L)
        assert(result[0].offerSearch.state == OfferResultAction.COMPLAIN)
        assert(result[0].offer.id == 1L)
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

        val result = offerSearchService.getOffersResult(strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.ACCEPT)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `all search item states with same searchRequestId and offerId should be same when one of them is updated`() {
        `should be create new offer search item and get result by clientId and search request id`()
        `client can complain to search item`()

        val result = offerSearchService.getSearchOffers(strategy, 1L, 1L).get()
        assert(result.size == 2)
        assert(result[0].id >= 1L)
        assert(result[0].state == OfferResultAction.COMPLAIN)
        assert(result[1].id >= 1L)
        assert(result[1].state == OfferResultAction.COMPLAIN)
    }

}
