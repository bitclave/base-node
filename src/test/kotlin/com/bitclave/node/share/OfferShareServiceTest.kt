package com.bitclave.node.share

import com.bitclave.node.ContractLoader
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferAction
import com.bitclave.node.repository.entities.OfferInteraction
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
import com.bitclave.node.repository.entities.OfferSearch
import com.bitclave.node.repository.entities.OfferShareData
import com.bitclave.node.repository.entities.SearchRequest
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
import com.bitclave.node.repository.search.interaction.OfferInteractionCrudRepository
import com.bitclave.node.repository.search.interaction.OfferInteractionRepositoryStrategy
import com.bitclave.node.repository.search.interaction.PostgresOfferInteractionRepositoryImpl
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepositoryStrategy
import com.bitclave.node.repository.search.offer.PostgresOfferSearchRepositoryImpl
import com.bitclave.node.repository.share.OfferShareCrudRepository
import com.bitclave.node.repository.share.OfferShareRepositoryStrategy
import com.bitclave.node.repository.share.PostgresOfferShareRepositoryImpl
import com.bitclave.node.services.events.WsService
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
import javax.persistence.EntityManager

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OfferShareServiceTest {

    companion object {
        private const val SHARE_DATA_RESPONSE = "SHARE_DATA_RESPONSE"
    }

    @Autowired
    private lateinit var contractLoader: ContractLoader

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository

    @Autowired
    protected lateinit var offerPriceCrudRepository: OfferPriceCrudRepository

    @Autowired
    protected lateinit var offerPriceRuleCrudRepository: OfferPriceRulesCrudRepository

    @Autowired
    protected lateinit var offerShareCrudRepository: OfferShareCrudRepository
    protected lateinit var offerShareService: OfferShareService

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository

    @Autowired
    protected lateinit var offerSearchCrudRepository: OfferSearchCrudRepository

    @Autowired
    protected lateinit var offerInteractionCrudRepository: OfferInteractionCrudRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var wsService: WsService

    private val accountClient: Account =
        Account("02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea")
    private val accountBusiness: Account =
        Account("03836649d2e353c332287e8280d1dbb1805cab0bae289ad08db9cc86f040ac6360")
    protected lateinit var strategy: RepositoryStrategyType

    private val offer = Offer(
        0,
        accountBusiness.publicKey,
        listOf(),
        "is desc",
        "is title",
        "is image url",
        BigDecimal.TEN.toString(),
        mapOf("car" to "true", "color" to "red"),
        mapOf("age" to "18", "salary" to "1000"),
        mapOf("age" to Offer.CompareAction.MORE_OR_EQUAL, "salary" to Offer.CompareAction.MORE_OR_EQUAL)
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

    @Before
    fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(contractLoader)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)
        val postgresOfferRepository =
            PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository, entityManager, wsService)
        val offerRepositoryStrategy = OfferRepositoryStrategy(postgresOfferRepository)

        val offerShareRepository = PostgresOfferShareRepositoryImpl(offerShareCrudRepository)
        val shareRepositoryStrategy = OfferShareRepositoryStrategy(offerShareRepository)

        val searchRequestRepository =
            PostgresSearchRequestRepositoryImpl(
                searchRequestCrudRepository,
                offerSearchCrudRepository,
                entityManager
            )
        val searchRequestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerSearchRepository = PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        val offerSearchStateRepository =
            PostgresOfferInteractionRepositoryImpl(offerInteractionCrudRepository, entityManager)
        val offerSearchStateRepositoryStrategy = OfferInteractionRepositoryStrategy(offerSearchStateRepository)

        val offerPriceRepository =
            PostgresOfferPriceRepositoryImpl(offerPriceCrudRepository, offerPriceRuleCrudRepository)
        val offerPriceRepositoryStrategy = OfferPriceRepositoryStrategy(offerPriceRepository)

        offerShareService = OfferShareService(
            shareRepositoryStrategy,
            offerRepositoryStrategy,
            offerSearchRepositoryStrategy,
            searchRequestRepositoryStrategy,
            offerSearchStateRepositoryStrategy
        )

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(accountClient, strategy)
        accountService.registrationClient(accountBusiness, strategy)

        offerRepositoryStrategy
            .changeStrategy(strategy)
            .saveOffer(offer)

        offerPrices = offerPriceRepositoryStrategy
            .changeStrategy(strategy)
            .savePrices(offer, listOf(offerPrice))

        val searchRequest = searchRequestRepositoryStrategy
            .changeStrategy(strategy)
            .save(SearchRequest(0, accountClient.publicKey, emptyMap()))

        offerSearchRepositoryStrategy
            .changeStrategy(strategy)
            .save(
                OfferSearch(
                    0,
                    searchRequest.owner,
                    searchRequest.id,
                    offer.id
                )
            )
        offerSearchStateRepository.repository.save(
            OfferInteraction(
                0,
                searchRequest.owner,
                offer.id,
                OfferAction.ACCEPT
            )
        )
    }

    @Test
    fun `should be create new share data`() {
        val projectId = offerPrices[0].id

        val originShareData = OfferShareData(
            42798414,
            accountBusiness.publicKey,
            accountClient.publicKey,
            SHARE_DATA_RESPONSE,
            BigDecimal.ZERO.toString(),
            true,
            projectId
        )

        offerShareService.grantAccess(accountClient.publicKey, originShareData, strategy).get()
    }

    @Test
    fun `should be business accept share data`() {
        `should be create new share data`()
        offerShareService.acceptShareData(
            accountBusiness.publicKey,
            42798414,
            BigDecimal("0.5"),
            strategy
        ).get()
    }

    @Test
    fun `should be find created share data`() {
        `should be create new share data`()
        val result = offerShareService.getShareData(
            accountBusiness.publicKey,
            null,
            strategy
        ).get()

        assertThat(result.size == 1)
        val shareData = result[0]
        assertThat(!shareData.accepted)
        assertThat(shareData.offerSearchId >= 1L)
        assertThat(shareData.offerOwner == accountBusiness.publicKey)
        assertThat(shareData.clientId == accountClient.publicKey)
        assertThat(shareData.clientResponse == SHARE_DATA_RESPONSE)
        assertThat(BigDecimal(shareData.worth) == BigDecimal.ZERO)
    }

    @Test
    fun `should return search requests by owner`() {
        `should be find created share data`()
        offerShareService.acceptShareData(
            accountBusiness.publicKey,
            42798414,
            BigDecimal("0.5"),
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
