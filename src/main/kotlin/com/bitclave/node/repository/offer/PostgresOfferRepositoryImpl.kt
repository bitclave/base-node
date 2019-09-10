package com.bitclave.node.repository.offer

import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import com.bitclave.node.services.events.OfferEvent
import com.bitclave.node.services.events.WsService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.repository.findByIdOrNull
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
    val entityManager: EntityManager,
    val wsService: WsService
) : OfferRepository {

    override fun saveOffer(offer: Offer): Offer {
        repository.save(offer) ?: throw DataNotSavedException()

        val result = syncElementCollections(offer)!!
        val event = if (offer.id > 0) OfferEvent.OnUpdate else OfferEvent.OnCreate

        // wsService.sendEvent(event, result)

        return result
    }

    override fun saveAll(offers: List<Offer>): List<Offer> {
        val result = repository.saveAll(offers).toList()
        return syncElementCollections(result)
    }

    override fun deleteOffer(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            val relatedOfferSearches = offerSearchRepository.findByOfferId(id)

            offerSearchRepository.deleteAll(relatedOfferSearches)

            wsService.sendEvent(OfferEvent.OnDelete, id)

            return id
        }

        return 0
    }

    override fun deleteOffers(owner: String): Int {
        val deletedOffers = repository.deleteByOwner(owner)
        deletedOffers.forEach {
            wsService.sendEvent(OfferEvent.OnDelete, it.id)
        }

        return deletedOffers.size
    }

    override fun findIdsByOwner(owner: String): List<Long> = repository.findIdsByOwner(owner).map { it.toLong() }

    override fun findByOwner(owner: String): List<Offer> {
        return syncElementCollections(repository.findByOwner(owner))
    }

    override fun findByOwner(owner: String, pageable: Pageable): Page<Offer> {
        return syncElementCollections(repository.findByOwner(owner, pageable))
    }

    override fun findById(id: Long): Offer? {
        return syncElementCollections(repository.findByIdOrNull(id))
    }

    override fun findByIds(ids: List<Long>, pageable: Pageable): Page<Offer> {
        return syncElementCollections(repository.findAllByIdIn(ids, pageable))
    }

    override fun findByIds(ids: List<Long>): List<Offer> {
        return syncElementCollections(repository.findAllById(ids).toList())
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

    override fun getAllOffersSlice(
        pageable: Pageable,
        syncCompare: Boolean,
        syncRules: Boolean,
        syncPrices: Boolean,
        exceptType: Offer.OfferType?
    ): Slice<Offer> {
        val slice = when (exceptType) {
            Offer.OfferType.PRODUCT -> repository.getAllOffersExceptProductsSlice(pageable)
            else -> repository.getAllOffersBy(pageable)
        }

        return syncElementCollections(
            slice,
            syncCompare,
            syncRules,
            syncPrices
        )
    }

    private fun syncElementCollections(offer: Offer?): Offer? {
        return if (offer == null) null else syncElementCollections(listOf(offer))[0]
    }

    private fun syncElementCollections(page: Page<Offer>): Page<Offer> {
        val result = syncElementCollections(page.content)
        val pageable = PageRequest.of(page.number, page.size, page.sort)

        return PageImpl(result, pageable, page.totalElements)
    }

    private fun syncElementCollections(
        slice: Slice<Offer>,
        syncCompare: Boolean = true,
        syncRules: Boolean = true,
        syncPrices: Boolean = true
    ): Slice<Offer> {
        val result = syncElementCollections(slice.content, syncCompare, syncRules, syncPrices)
        val pageable = PageRequest.of(slice.number, slice.size, slice.sort)

        return SliceImpl(result, pageable, slice.hasNext())
    }

    private fun syncElementCollections(
        offers: List<Offer>,
        syncCompare: Boolean = true,
        syncRules: Boolean = true,
        syncPrices: Boolean = true
    ): List<Offer> {
        val ids = offers.map { it.id }.distinct().joinToString(",")

        if (ids.isEmpty()) {
            return emptyList()
        }

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
            queryResultCompare = if (syncCompare) {
                entityManager
                    .createNativeQuery("SELECT * FROM offer_compare WHERE offer_id in ($ids);")
                    .resultList as List<Array<Any>>
            } else {
                emptyList()
            }

            @Suppress("UNCHECKED_CAST")
            queryResultRules = if (syncRules) {
                entityManager
                    .createNativeQuery("SELECT * FROM offer_rules WHERE offer_id in ($ids);")
                    .resultList as List<Array<Any>>
            } else {
                emptyList()
            }

            @Suppress("UNCHECKED_CAST")
            queryResultPrices = if (syncPrices) {
                entityManager
                    .createNativeQuery("SELECT * FROM offer_price WHERE offer_id in ($ids);", OfferPrice::class.java)
                    .resultList as List<OfferPrice>
            } else {
                emptyList()
            }
        }

        // println("syncElementCollections get raw result objects: $step1")

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
        // println("syncElementCollections mapping ids: $step2")

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

        // println("syncElementCollections merge result: $mergeResult")

        return result
    }

    private fun syncPriceRules(offerPriceIds: List<Long>): Map<Long, List<OfferPriceRules>> {
        val ids = offerPriceIds.joinToString(",")
        var result = mapOf<Long, List<OfferPriceRules>>()

        if (offerPriceIds.isEmpty()) {
            return result
        }

        val loadPrices = measureTimeMillis {
            @Suppress("UNCHECKED_CAST")
            result = (entityManager
                .createNativeQuery(
                    "SELECT * FROM offer_price_rules WHERE offer_price_id in ($ids);",
                    OfferPriceRules::class.java
                )
                .resultList as List<OfferPriceRules>).groupBy { it.originalOfferPriceId }
        }
        // println("syncElementCollections syncPriceRules: $loadPrices")

        return result
    }
}
