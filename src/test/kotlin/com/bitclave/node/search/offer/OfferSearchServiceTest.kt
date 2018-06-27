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
            "desc",
            "title",
            "url"
    )

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

        offerRepositoryStrategy.changeStrategy(strategy)
                .saveOffer(offer)

        searchRequestRepositoryStrategy.changeStrategy(strategy)
                .saveSearchRequest(SearchRequest(0, publicKey, emptyMap()))
    }

    @Test
    fun `should be create new offer search item and get result by clientId and search request id`() {
        offerSearchService.saveOfferSearch(
                publicKey,
                OfferSearch(0, 1L, 1L, OfferResultAction.NONE),
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(publicKey, strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test
    fun `should be create new offer search item and get result by clientId and offer search id`() {
        offerSearchService.saveOfferSearch(
                publicKey,
                OfferSearch(0, 1L, 1L, OfferResultAction.NONE),
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(publicKey, strategy, null, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.NONE)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `client can complain to search item`() {
        `should be create new offer search item and get result by clientId and search request id`()

        offerSearchService.complain(publicKey, 1L, strategy).get()

        val result = offerSearchService.getOffersResult(publicKey, strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.REJECT)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

    @Test fun `search item state should be ACCEPT`() {
        `should be create new offer search item and get result by clientId and search request id`()

        offerShareService.grantAccess(
                publicKey,
                OfferShareData(1L, businessPublicKey, publicKey, "response"),
                strategy
        ).get()

        val result = offerSearchService.getOffersResult(publicKey, strategy, 1L).get()
        assert(result.size == 1)
        assert(result[0].offerSearch.id == 1L)
        assert(result[0].offerSearch.state == OfferResultAction.ACCEPT)
        assert(result[0].offer.id == 1L)
        assert(result[0].offer.owner == businessPublicKey)
    }

}
