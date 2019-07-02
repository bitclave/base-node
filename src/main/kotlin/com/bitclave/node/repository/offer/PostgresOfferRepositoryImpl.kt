package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferPrice
import com.bitclave.node.repository.models.OfferPriceRules
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.HashMap
import javax.persistence.EntityManager
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresOfferRepositoryImpl(
    val repository: OfferCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository,
    val entityManager: EntityManager
) : OfferRepository {

    private val logger = KotlinLogging.logger {}

    override fun saveOffer(offer: Offer): Offer {
        val id = offer.id
        repository.save(offer) ?: throw DataNotSavedException()
        if (id > 0) {
            var deletedOfferSearchCount = 0
            val step1 = measureTimeMillis {
                deletedOfferSearchCount = offerSearchRepository.deleteAllByOfferIdAndStateIn(offer.id)
            }
            logger.debug { "saveOffer: step 1: ms: $step1, l1: $deletedOfferSearchCount" }
        }

        return syncElementCollections(offer)!!
    }

    override fun shallowSaveOffer(offer: Offer): Offer {
        repository.save(offer) ?: throw DataNotSavedException()
        return syncElementCollections(offer)!!
    }

    override fun deleteOffer(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            val relatedOfferSearches = offerSearchRepository.findByOfferId(id)

            offerSearchRepository.delete(relatedOfferSearches)

            return id
        }

        return 0
    }

    override fun deleteOffers(owner: String): Long {
        return repository.deleteByOwner(owner)
    }

    override fun findByOwner(owner: String): List<Offer> {
        return syncElementCollections(repository.findByOwner(owner))
    }

    override fun findByOwner(owner: String, pageable: Pageable): Page<Offer> {
        return syncElementCollections(repository.findByOwner(owner, pageable))
    }

    override fun findById(id: Long): Offer? {
        return syncElementCollections(repository.findById(id))
    }

    override fun findByIds(ids: List<Long>, pageable: Pageable): Page<Offer> {
        return syncElementCollections(repository.findAllByIdIn(ids, pageable))
    }

    override fun findByIds(ids: List<Long>): List<Offer> {
        return syncElementCollections(repository.findAll(ids).toList())
    }

    override fun findByIdAndOwner(id: Long, owner: String): Offer? {
        return syncElementCollections(repository.findByIdAndOwner(id, owner))
    }

    override fun findAll(): List<Offer> {
        return syncElementCollections(repository.findAll().toList())
    }

    override fun findAll(pageable: Pageable): Page<Offer> {
        var result: Page<Offer>? = null
        val step1 = measureTimeMillis {
            result = repository.findAll(pageable)
        }

        println("syncElementCollections get pageable: $step1")

        return syncElementCollections(result!!)
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun getOfferByOwnerAndTag(owner: String, tagKey: String): List<Offer> {
        return syncElementCollections(repository.getOfferByOwnerAndTag(owner, tagKey))
    }

    override fun getAllOffersExceptProducts(pageable: Pageable): Page<Offer> {
        return syncElementCollections(repository.getAllOffersExceptProducts(pageable))
    }

    private fun syncElementCollections(offer: Offer?): Offer? {
        return if (offer == null) null else syncElementCollections(listOf(offer))[0]
    }

    private fun syncElementCollections(page: Page<Offer>): Page<Offer> {
        val result = syncElementCollections(page.content)
        val pageable = PageRequest(page.number, page.size, page.sort)

        return PageImpl(result, pageable, result.size.toLong())
    }

    private fun syncElementCollections(offers: List<Offer>): List<Offer> {
        val ids = offers.map { it.id }.distinct().joinToString(",")

        var queryResultTags = emptyList<Array<Any>>()
        var queryResultCompare = emptyList<Array<Any>>()
        var queryResultRules = emptyList<Array<Any>>()
        var queryResultPrices = emptyList<OfferPrice>()

        val step1 = measureTimeMillis {
            @Suppress("UNCHECKED_CAST")
            queryResultTags = entityManager
                .createNativeQuery("SELECT * FROM offer_tags WHERE offer_id in ($ids);")
                .resultList as List<Array<Any>>

            @Suppress("UNCHECKED_CAST")
            queryResultCompare = entityManager
                .createNativeQuery("SELECT * FROM offer_compare WHERE offer_id in ($ids);")
                .resultList as List<Array<Any>>

            @Suppress("UNCHECKED_CAST")
            queryResultRules = entityManager
                .createNativeQuery("SELECT * FROM offer_rules WHERE offer_id in ($ids);")
                .resultList as List<Array<Any>>

            @Suppress("UNCHECKED_CAST")
            queryResultPrices = entityManager
                .createNativeQuery("SELECT * FROM offer_price WHERE offer_id in ($ids);", OfferPrice::class.java)
                .resultList as List<OfferPrice>
        }

        println("syncElementCollections get raw result objects: $step1")

        var mappedTags = emptyMap<Long, List<Array<Any>>>()
        var mappedCompare = emptyMap<Long, List<Array<Any>>>()
        var mappedRules = emptyMap<Long, List<Array<Any>>>()
        var mappedPrices = emptyMap<Long, List<OfferPrice>>()
        var priceIds = emptyList<Long>()

        val step2 = measureTimeMillis {
            mappedTags = (queryResultTags).groupBy { (it[0] as BigInteger).toLong() }
            mappedCompare = (queryResultCompare).groupBy { (it[0] as BigInteger).toLong() }
            mappedRules = (queryResultRules).groupBy { (it[0] as BigInteger).toLong() }
            mappedPrices = (queryResultPrices).groupBy { it.originalOfferId }
            priceIds = queryResultPrices.map { it.id }.distinct()
        }
        println("syncElementCollections mapping ids: $step2")

        return mergeOfferWithMaps(offers, mappedTags, mappedCompare, mappedRules, mappedPrices, priceIds)
    }

    private fun mergeOfferWithMaps(
        offers: List<Offer>,
        mapTags: Map<Long, List<Array<Any>>>,
        mapCompare: Map<Long, List<Array<Any>>>,
        mapRules: Map<Long, List<Array<Any>>>,
        mapPrices: Map<Long, List<OfferPrice>>,
        pricesIds: List<Long>
    ): List<Offer> {
        var result = emptyList<Offer>()
        val mapPricesRules = syncPriceRules(pricesIds)

        val mergeResult = measureTimeMillis {
            result = offers.map {
                val tags = HashMap<String, String>()
                val compare = HashMap<String, String>()
                val rules = HashMap<String, Offer.CompareAction>()
                val prices = (mapPrices[it.id] ?: emptyList())
                    .map { price -> price.copy(rules = mapPricesRules[price.id] ?: emptyList()) }

                mapTags[it.id]?.forEach { rawTag -> tags[rawTag[2] as String] = rawTag[1] as String }
                mapCompare[it.id]?.forEach { rawCompare -> compare[rawCompare[2] as String] = rawCompare[1] as String }
                mapRules[it.id]?.forEach { rawCompare ->
                    rules[rawCompare[2] as String] = Offer.CompareAction.values()[rawCompare[1] as Int]
                }

                return@map it.copy(tags = tags, compare = compare, rules = rules, offerPrices = prices)
            }
        }

        println("syncElementCollections merge result: $mergeResult")

        return result
    }

    private fun syncPriceRules(offerPriceIds: List<Long>): Map<Long, List<OfferPriceRules>> {
        val ids = offerPriceIds.joinToString(",")
        @Suppress("UNCHECKED_CAST")
        return (entityManager
            .createNativeQuery(
                "SELECT * FROM offer_price_rules WHERE offer_price_id in ($ids);",
                OfferPriceRules::class.java
            )
            .resultList as List<OfferPriceRules>).groupBy { it.originalOfferPriceId }
    }
}
