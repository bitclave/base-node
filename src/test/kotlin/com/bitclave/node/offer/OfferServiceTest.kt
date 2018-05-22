package com.bitclave.node.offer

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.offer.OfferCrudRepository
import com.bitclave.node.repository.offer.OfferRepositoryStrategy
import com.bitclave.node.repository.offer.PostgresOfferRepositoryImpl
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.OfferService
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
class OfferServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var offerCrudRepository: OfferCrudRepository
    protected lateinit var offerService: OfferService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val offer = Offer(
            0,
            account.publicKey,
            "is desc",
            "is title",
            "is image url",
            BigDecimal.TEN.toString(),
            mapOf("car" to "true", "color" to "red"),
            mapOf("age" to "18", "salary" to "1000"),
            mapOf("age" to Offer.CompareAction.MORE_OR_EQUAL, "salary" to Offer.CompareAction.MORE_OR_EQUAL)
    )

    @Before fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)
        val postgresOfferRepository = PostgresOfferRepositoryImpl(offerCrudRepository)
        val offerServiceStrategy = OfferRepositoryStrategy(postgresOfferRepository)

        offerService = OfferService(offerServiceStrategy)

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)
    }

    @Test fun `should be create new offer`() {
        val result = offerService.putOffer(0, account.publicKey, offer, strategy).get()
        assert(result.id >= 1L)
        assertThat(result.owner).isEqualTo(account.publicKey)
        assertThat(result.description).isEqualTo(offer.description)
        assertThat(result.title).isEqualTo(offer.title)
        assertThat(result.imageUrl).isEqualTo(offer.imageUrl)
        assertThat(result.tags).isEqualTo(offer.tags)
        assertThat(result.compare).isEqualTo(offer.compare)
        assertThat(result.rules).isEqualTo(offer.rules)
    }

    @Test fun `should be update created offer`() {
        val changedOffer = Offer(
                34,
                "dsdsdsdsd",
                "is desc111",
                "is title111",
                "is image url111",
                BigDecimal.ONE.toString(),
                mapOf("color" to "red"),
                mapOf("salary" to "1000"),
                mapOf("salary" to Offer.CompareAction.MORE))

        val created = offerService.putOffer(0, account.publicKey, offer, strategy).get()

        assert(created.id == 1L)
        val updated = offerService.putOffer(created.id, account.publicKey, changedOffer, strategy).get()

        assert(updated.id == 1L)
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
    }

    @Test fun `should delete existed offer`() {
        `should be create new offer`()

        var savedListResult = offerService.getOffers(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(offer, "id")

        val deletedId = offerService.deleteOffer(1, account.publicKey, strategy).get()

        assert(deletedId == 1L)

        savedListResult = offerService.getOffers(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test fun `should delete existed offers`() {
        `should be create new offer`()
        `should be create new offer`()
        `should be create new offer`()

        var savedListResult = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(3)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(offer, "id")
        assertThat(savedListResult[1]).isEqualToIgnoringGivenFields(offer, "id")
        assertThat(savedListResult[2]).isEqualToIgnoringGivenFields(offer, "id")

        offerService.deleteOffers(account.publicKey, strategy).get()

        savedListResult = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test fun `should return offers by id and owner`() {
        `should be create new offer`()
        `should be create new offer`()

        var result = offerService.getOffers(1, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, "id")

        result = offerService.getOffers(2, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, "id")

        result = offerService.getOffers(3, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(0)
    }

    @Test fun `should return offers by owner`() {
        `should be create new offer`()
        `should be create new offer`()

        val result = offerService.getOffers(0, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(2)
        assert(result[0].id == 1L)
        assert(result[1].id == 2L)
        assertThat(result[0]).isEqualToIgnoringGivenFields(offer, "id")
        assertThat(result[1]).isEqualToIgnoringGivenFields(offer, "id")
    }

}
