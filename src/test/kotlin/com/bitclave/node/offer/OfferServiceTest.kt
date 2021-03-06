package com.bitclave.node.offer

import com.bitclave.node.ContractLoader
import com.bitclave.node.configuration.properties.AppOpticsProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
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
import com.bitclave.node.services.events.WsService
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferSearchService
import com.bitclave.node.services.v1.OfferService
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import javax.persistence.EntityManager

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OfferServiceTest {

    @Autowired
    private lateinit var contractLoader: ContractLoader

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository
    protected lateinit var offerService: OfferService

    @Autowired
    protected lateinit var offerPriceCrudRepository: OfferPriceCrudRepository

    @Autowired
    protected lateinit var offerPriceRulesCrudRepository: OfferPriceRulesCrudRepository

    @Autowired
    protected lateinit var offerSearchCrudRepository: OfferSearchCrudRepository

    @Autowired
    protected lateinit var querySearchRequestCrudRepository: QuerySearchRequestCrudRepository

    @Autowired
    protected lateinit var offerRankCrudRepository: OfferRankCrudRepository

    protected val rtSearchRepository = Mockito.mock(RtSearchRepositoryImpl::class.java)

    @Autowired
    protected lateinit var offerInteractionCrudRepository: OfferInteractionCrudRepository

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository

    @Autowired
    private lateinit var gson: Gson

    @Autowired
    private lateinit var appOpticsProperties: AppOpticsProperties

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var wsService: WsService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val ignoreFields = arrayOf("id", "offerPrices", "updatedAt", "createdAt")

    private fun getOriginPrices(): List<OfferPrice> {
        return listOf(
            OfferPrice(
                0,
                "first price description",
                BigDecimal("0.5").toString(),
                listOf(
                    OfferPriceRules(0, "age", "10"),
                    OfferPriceRules(0, "sex", "male"),
                    OfferPriceRules(0, "country", "USA")
                )
            ),
            OfferPrice(
                0,
                "second price description",
                BigDecimal("0.7").toString(),
                listOf(
                    OfferPriceRules(0, "age", "20"),
                    OfferPriceRules(0, "sex", "female"),
                    OfferPriceRules(0, "country", "England")
                )
            ),
            OfferPrice(
                0,
                "third price description",
                BigDecimal("0.9").toString(),
                listOf(
                    OfferPriceRules(0, "age", "30"),
                    OfferPriceRules(0, "sex", "male"),
                    OfferPriceRules(0, "country", "Israel")
                )
            ),
            OfferPrice(
                0,
                "fourth price description",
                BigDecimal("1.2").toString(),
                listOf(
                    OfferPriceRules(0, "age", "40"),
                    OfferPriceRules(0, "sex", "male"),
                    OfferPriceRules(0, "country", "Ukraine")
                )
            )
        )
    }

    private val offer = Offer(
        0,
        account.publicKey,
        listOf(),
        "is desc",
        "is title",
        "is image url",
        BigDecimal.TEN.toString(),
        mapOf("car" to "true", "color" to "red"),
        mapOf("age" to "18", "salary" to "1000"),
        mapOf("age" to Offer.CompareAction.MORE_OR_EQUAL, "salary" to Offer.CompareAction.MORE_OR_EQUAL)
    )

    @Before
    fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(contractLoader)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)

        val postgresOfferRepository =
            PostgresOfferRepositoryImpl(offerCrudRepository, offerSearchCrudRepository, entityManager, wsService)
        val offerServiceStrategy = OfferRepositoryStrategy(postgresOfferRepository)

        val postgresOfferPriceRepository =
            PostgresOfferPriceRepositoryImpl(offerPriceCrudRepository, offerPriceRulesCrudRepository)
        val offerPriceServiceStrategy = OfferPriceRepositoryStrategy(postgresOfferPriceRepository)

        val searchRequestRepository =
            PostgresSearchRequestRepositoryImpl(
                searchRequestCrudRepository,
                offerSearchCrudRepository,
                entityManager
            )
        val requestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        val offerSearchRepository =
            PostgresOfferSearchRepositoryImpl(offerSearchCrudRepository)
        val offerSearchRepositoryStrategy = OfferSearchRepositoryStrategy(offerSearchRepository)

        val offerInteractionRepository =
            PostgresOfferInteractionRepositoryImpl(offerInteractionCrudRepository, entityManager)
        val offerInteractionRepositoryStrategy = OfferInteractionRepositoryStrategy(offerInteractionRepository)

        val offerRankStateRepository = PostgresOfferRankRepositoryImpl(offerRankCrudRepository)
        val offerRankRepositoryStrategy = OfferRankRepositoryStrategy(offerRankStateRepository)

        val offerSearchService = OfferSearchService(
            requestRepositoryStrategy,
            offerServiceStrategy,
            offerSearchRepositoryStrategy,
            querySearchRequestCrudRepository,
            rtSearchRepository,
            offerInteractionRepositoryStrategy,
            gson,
            appOpticsProperties
        )

        offerService = OfferService(
            offerServiceStrategy,
            offerPriceServiceStrategy,
            offerRankRepositoryStrategy,
            offerSearchService
        )

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)
    }

    @Test
    fun `should be create new offer`() {

        val oneOffer = this.offer.copy(offerPrices = getOriginPrices())

        val result = offerService.putOffer(0, account.publicKey, oneOffer, strategy).get()

        assert(result.id >= 1L)
        assertThat(result.owner).isEqualTo(account.publicKey)
        assertThat(result.description).isEqualTo(offer.description)
        assertThat(result.title).isEqualTo(offer.title)
        assertThat(result.imageUrl).isEqualTo(offer.imageUrl)
        assertThat(result.tags).isEqualTo(offer.tags)
        assertThat(result.compare).isEqualTo(offer.compare)
        assertThat(result.rules).isEqualTo(offer.rules)

        assertThat(result.offerPrices.size).isEqualTo(4)
    }

    @Test
    fun `should be update created offer without prices`() {
        val changedOffer = Offer(
            34,
            "dsdsdsdsd",
            listOf(),
            "is desc111",
            "is title111",
            "is image url111",
            BigDecimal.ONE.toString(),
            mapOf("color" to "red"),
            mapOf("salary" to "1000"),
            mapOf("salary" to Offer.CompareAction.MORE)
        )

        val created = offerService.putOffer(0, account.publicKey, offer, strategy).get()

        assert(created.id >= 1L)
        val updated = offerService.putOffer(created.id, account.publicKey, changedOffer, strategy).get()

        assert(updated.id == created.id)
        assertThat(updated.owner).isEqualTo(account.publicKey)
        assertThat(updated.id).isNotEqualTo(changedOffer.id)
        assertThat(updated.owner).isNotEqualTo(changedOffer.owner)

        assertThat(updated.description).isEqualTo(changedOffer.description)
        assertThat(updated.title).isEqualTo(changedOffer.title)
        assertThat(updated.imageUrl).isEqualTo(changedOffer.imageUrl)
        assertThat(updated.tags).isEqualTo(changedOffer.tags)
        assertThat(updated.worth).isEqualTo(BigDecimal.ONE.toString())
        assertThat(updated.compare).isEqualTo(changedOffer.compare)
        assertThat(updated.rules).isEqualTo(changedOffer.rules)
        assertThat(updated.createdAt.time).isEqualTo(created.createdAt.time)
        assertThat(updated.updatedAt.time > created.updatedAt.time)
    }

    @Test
    fun `should be update created offer with prices`() {
        val oneOffer = this.offer.copy(offerPrices = getOriginPrices())

        var changedOffer = Offer(
            34,
            "dsdsdsdsd",
            getOriginPrices(),
            "is desc111",
            "is title111",
            "is image url111",
            BigDecimal.ONE.toString(),
            mapOf("color" to "red"),
            mapOf("salary" to "1000"),
            mapOf("salary" to Offer.CompareAction.MORE)
        )

        val created = offerService.putOffer(0, account.publicKey, oneOffer, strategy).get()

        assert(created.id >= 1L)

        val updatedDescription = "updated"
        changedOffer = changedOffer.copy(offerPrices = created.offerPrices)

        changedOffer.offerPrices.find { it.id == 25766485L }?.description = updatedDescription

        val updated = offerService.putOffer(created.id, account.publicKey, changedOffer, strategy).get()

        assert(updated.id == created.id)
        assertThat(updated.owner).isEqualTo(account.publicKey)
        assertThat(updated.id).isNotEqualTo(changedOffer.id)
        assertThat(updated.owner).isNotEqualTo(changedOffer.owner)

        assertThat(updated.offerPrices.find { it.id == 25766485L }!!.description).isEqualTo(updatedDescription)

        assertThat(updated.description).isEqualTo(changedOffer.description)
        assertThat(updated.title).isEqualTo(changedOffer.title)
        assertThat(updated.imageUrl).isEqualTo(changedOffer.imageUrl)
        assertThat(updated.tags).isEqualTo(changedOffer.tags)
        assertThat(updated.worth).isEqualTo(BigDecimal.ONE.toString())
        assertThat(updated.compare).isEqualTo(changedOffer.compare)
        assertThat(updated.rules).isEqualTo(changedOffer.rules)
        assertThat(updated.createdAt.time).isEqualTo(created.createdAt.time)
    }

    @Test
    fun `should delete existed offer`() {
        `should be create new offer`()

        var savedListResult = offerService.getOffers(17239325, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(offer, *ignoreFields)

        val deletedId = offerService.deleteOffer(17239325, account.publicKey, strategy).get()

        assert(deletedId == 17239325L)

        savedListResult = offerService.getOffers(17239325, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test
    fun `should delete existed offers`() {
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()

        var savedListResult = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(3)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(offer, *ignoreFields)
        assertThat(savedListResult[1]).isEqualToIgnoringGivenFields(offer, *ignoreFields)
        assertThat(savedListResult[2]).isEqualToIgnoringGivenFields(offer, *ignoreFields)

        offerService.deleteOffers(account.publicKey, strategy).get()

        savedListResult = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test
    fun `should return offers by id and owner`() {
        `should be create new offer`()
        `should be create new offer`()

        var result = offerService.getOffers(17239325, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, *ignoreFields)

        result = offerService.getOffers(17239326, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, *ignoreFields)

        result = offerService.getOffers(17239327, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `should return offers by owner`() {
        `should be create new offer`()
        `should be create new offer`()

        val result = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(2)
        assert(result[0].id >= 1L)
        assert(result[1].id > result[0].id)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, *ignoreFields)
        assertThat(result[1]).isEqualToIgnoringGivenFields(offer, *ignoreFields)
    }

    @Test
    fun `should return all offers by page`() {
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()

        val firstPage = offerService.getPageableOffers(PageRequest.of(0, 2), strategy).get()
        assertThat(firstPage.size).isEqualTo(2)
        assert(firstPage.first().id >= 1L)
        assert(firstPage.last().id > firstPage.first().id)

        val secondPage = offerService.getPageableOffers(PageRequest.of(1, 2), strategy).get()
        assertThat(secondPage.size).isEqualTo(2)
        assert(secondPage.first().id > firstPage.last().id)
        assert(secondPage.last().id > secondPage.first().id)
    }

    @Test
    fun `get total count of offers`() {
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()

        val result = offerService.getOfferTotalCount(strategy).get()
        assert(result == 4L)
    }

    @Test
    fun `should return offers by owner and tag`() {
        `should be create new offer`()
        `should be create new offer`()

        var result = offerService.getOfferByOwnerAndTag(account.publicKey, "car", strategy).get()
        assertThat(result.size).isEqualTo(2)

        result = offerService.getOfferByOwnerAndTag(account.publicKey, "age", strategy).get()
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `should return all offers except products by page for matcher`() {
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()

        val firstPage = offerService.getPageableOffersForMatcher(PageRequest.of(0, 2), strategy).get()
        assertThat(firstPage.size).isEqualTo(2)
        assert(firstPage.first().id >= 1L)
        assert(firstPage.last().id > firstPage.first().id)

        val secondPage = offerService.getPageableOffersForMatcher(PageRequest.of(1, 2), strategy).get()
        assertThat(secondPage.size).isEqualTo(2)
        assert(secondPage.first().id > firstPage.last().id)
        assert(secondPage.last().id > secondPage.first().id)
    }

    @Test
    fun `should return offers without owner`() {

        `should be create new offer`()

        val changedOffer = Offer(
            34,
            "dsdsdsdsd",
            listOf(),
            "is desc111",
            "is title111",
            "is image url111",
            BigDecimal.ONE.toString(),
            mapOf("color" to "red"),
            mapOf("salary" to "1000"),
            mapOf("salary" to Offer.CompareAction.MORE)
        )

        val created = offerService.putOffer(0, "dsdsdsdsd", changedOffer, strategy).get()

        assert(created.id == 17239326L)

        val result = offerService.getOffersWithoutOwner(strategy).get()
        assertThat(result.size).isEqualTo(1)
    }
}
