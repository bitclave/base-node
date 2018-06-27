package com.bitclave.node.share

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
import com.bitclave.node.services.v1.OfferShareService
import org.assertj.core.api.Assertions.assertThat
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
class OfferShareServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository

    @Autowired
    protected lateinit var offerShareCrudRepository: OfferShareCrudRepository
    protected lateinit var offerShareService: OfferShareService

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository

    @Autowired
    protected lateinit var offerSearchCrudRepository: OfferSearchCrudRepository

    private val accountClient: Account =
            Account("02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea")
    private val accountBusiness: Account =
            Account("03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360")
    protected lateinit var strategy: RepositoryStrategyType

    private val offer = Offer(
            0,
            accountBusiness.publicKey,
            "is desc",
            "is title",
            "is image url",
            BigDecimal.TEN.toString(),
            mapOf("car" to "true", "color" to "red"),
            mapOf("age" to "18", "salary" to "1000"),
            mapOf("age" to Offer.CompareAction.MORE_OR_EQUAL, "salary" to Offer.CompareAction.MORE_OR_EQUAL)
    )

    private val SHARE_DATA_RESPONSE = "SHARE_DATA_RESPONSE"

    @Before fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)
        val postgresOfferRepository = PostgresOfferRepositoryImpl(offerCrudRepository)
        val offerRepositoryStrategy = OfferRepositoryStrategy(postgresOfferRepository)

        val offerShareRepository = PostgresOfferShareRepositoryImpl(offerShareCrudRepository)
        val shareRepositoryStrategy = OfferShareRepositoryStrategy(offerShareRepository)

        val searchRequestRepository = PostgresSearchRequestRepositoryImpl(searchRequestCrudRepository)
        val searchRequestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerSearchRepository = PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        offerShareService = OfferShareService(
                shareRepositoryStrategy,
                offerRepositoryStrategy,
                offerSearchRepositoryStrategy,
                searchRequestRepositoryStrategy
        )

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(accountClient, strategy)
        accountService.registrationClient(accountBusiness, strategy)
        offerRepositoryStrategy.changeStrategy(strategy).saveOffer(offer)

        val searchRequest = searchRequestRepositoryStrategy.changeStrategy(strategy)
                .saveSearchRequest(SearchRequest(0, accountClient.publicKey, emptyMap()))

        offerSearchRepositoryStrategy.changeStrategy(strategy)
                .saveSearchResult(OfferSearch(0, searchRequest.id, 1))
    }

    @Test fun `should be create new share data`() {
        val originShareData = OfferShareData(
                1L,
                accountBusiness.publicKey,
                accountClient.publicKey,
                SHARE_DATA_RESPONSE
        )

        offerShareService.grantAccess(accountClient.publicKey, originShareData, strategy).get()
    }

    @Test fun `should be business accept share data`() {
        `should be create new share data`()
        offerShareService.acceptShareData(
                accountBusiness.publicKey,
                1,
                BigDecimal.TEN,
                strategy
        ).get()
    }

    @Test fun `should be find created share data`() {
        `should be create new share data`()
        val result = offerShareService.getShareData(
                accountBusiness.publicKey,
                null,
                strategy
        ).get()

        assertThat(result.size == 1)
        val shareData = result[0]
        assertThat(!shareData.accepted)
        assertThat(shareData.offerSearchId == 1L)
        assertThat(shareData.offerOwner == accountBusiness.publicKey)
        assertThat(shareData.clientId == accountClient.publicKey)
        assertThat(shareData.clientResponse == SHARE_DATA_RESPONSE)
        assertThat(BigDecimal(shareData.worth) == BigDecimal.ZERO)
    }

    @Test fun `should return search requests by owner`() {
        `should be find created share data`()
        offerShareService.acceptShareData(
                accountBusiness.publicKey,
                1,
                BigDecimal.TEN,
                strategy
        ).get()
        var result = offerShareService.getShareData(
                accountBusiness.publicKey,
                false,
                strategy
        ).get()
        assertThat(result.isEmpty())

        result = offerShareService.getShareData(
                accountBusiness.publicKey,
                null,
                strategy
        ).get()

        assertThat(result.size == 1)
        val shareData = result[0]

        assertThat(shareData.accepted)
        assertThat(shareData.offerSearchId == 1L)
        assertThat(shareData.offerOwner == accountBusiness.publicKey)
        assertThat(shareData.clientId == accountClient.publicKey)
        assertThat(shareData.clientResponse == SHARE_DATA_RESPONSE)
        assertThat(BigDecimal(shareData.worth) == BigDecimal.TEN)
    }

}
