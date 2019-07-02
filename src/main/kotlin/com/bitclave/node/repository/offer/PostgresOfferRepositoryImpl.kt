package com.bitclave.node.repository.offer

import com.bitclave.node.repository.models.Offer
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
        return syncElementCollections(repository.findAll(pageable))
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
        return offer ?: syncElementCollections(listOf(offer!!))[0]
    }

    private fun syncElementCollections(page: Page<Offer>): Page<Offer> {
        val result = syncElementCollections(page.content)
        val pageable = PageRequest(page.number, page.size, page.sort)

        return PageImpl(result, pageable, result.size.toLong())
    }

    private fun syncElementCollections(offers: List<Offer>): List<Offer> {
        val ids = offers.map { it.id }.distinct().joinToString(",")

        var queryResult = emptyList<Array<Any?>>()

        val step1 = measureTimeMillis {
            @Suppress("UNCHECKED_CAST")
            queryResult = entityManager.createNativeQuery(
                "SELECT *" +
                    "FROM offer_tags" +
                    " NATURAL RIGHT OUTER JOIN offer_rules " +
                    " NATURAL RIGHT OUTER JOIN offer_compare" +
                    " WHERE offer_id in ($ids);"
            ).resultList as List<Array<Any?>>
        }

        println("syncElementCollections get raw result objects: $step1")

        var mappedById = emptyMap<Long, List<Array<Any?>>>()
        val step2 = measureTimeMillis {
            mappedById = (queryResult).groupBy { (it[0] as BigInteger).toLong() }
        }
        println("syncElementCollections mapping ids: $step2")

        var result = emptyList<Offer>()

        val mergeResult = measureTimeMillis {
            result = offers.map {
                val queryItems = mappedById[it.id]
                if (queryItems != null) {
                    val tags = HashMap<String, String>()
                    val compare = HashMap<String, String>()
                    val rules = HashMap<String, Offer.CompareAction>()

                    queryItems.forEach { item ->
                        if (item[2] != null) {
                            tags[item[2] as String] = item[1] as String
                        }
                        if (item[4] != null) {
                            rules[item[4] as String] = Offer.CompareAction.values()[item[3] as Int]
                        }
                        if (item[6] != null) {
                            compare[item[6] as String] = item[5] as String
                        }
                    }
                    return@map it.copy(tags = tags, compare = compare, rules = rules)
                }

                return@map it
            }
        }

        println("syncElementCollections merge result: $mergeResult")
        return result
    }
}
