package com.bitclave.node.repository.offer

import com.bitclave.node.extensions.changeOrderFieldsToCamelCase
import com.bitclave.node.extensions.changeOrderFieldsToSnakeCase
import com.bitclave.node.repository.entities.Offer
import com.bitclave.node.repository.entities.OfferPrice
import com.bitclave.node.repository.entities.OfferPriceRules
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import com.bitclave.node.services.events.OfferEvent
import com.bitclave.node.services.events.WsService
import com.bitclave.node.utils.Logger
import com.bitclave.node.utils.LoggerType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.text.SimpleDateFormat
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

        wsService.sendEvent(event, result)

        return result
    }
    override fun saveAll(offers: List<Offer>): List<Offer> {
        val result = repository.saveAll(offers).toList()
        return syncElementCollections(result)
    }

    @Transactional
    override fun createAll(offers: List<Offer>): List<Offer> {
        var result: List<Offer> = listOf()
        val values = offers.map {
            val isoPattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            val updatedAt = SimpleDateFormat(isoPattern).format(it.updatedAt)
            val createdAt = SimpleDateFormat(isoPattern).format(it.createdAt)
            val id = if (it.id == 0L) "nextval('offer_id_seq')" else it.id.toString()

            "($id, '${safe(it.title)}', '${safe(it.description)}', '${safe(it.owner)}', " +
                "'${it.imageUrl}', '${it.worth}', '$createdAt', '$updatedAt')"
        }
        if (values.isNotEmpty()) {
            val insertOffers = "INSERT INTO offer " +
                "(id, title, description, owner, image_url, worth, created_at, updated_at) VALUES \n"
            val insertOffersQuery = insertOffers + values.joinToString(",\n") + "\nRETURNING id;"

            @Suppress("UNCHECKED_CAST")
            val insertedOfferIds: List<Long> = entityManager
                .createNativeQuery(insertOffersQuery).resultList as List<Long>

            val insertTags = "INSERT INTO offer_tags (offer_id, tags, tags_key) VALUES \n"
            val insertedOfferTagsValues = offers.mapIndexed { index, offer ->
                offer.tags.map { "( ${insertedOfferIds[index]}, '${safe(it.value)}', '${safe(it.key)}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferTagsValues.isNotEmpty()) {
                val insertOfferTagsQuery = insertTags + insertedOfferTagsValues
                entityManager.createNativeQuery(insertOfferTagsQuery).executeUpdate()
            }

            val insertCompare = "INSERT INTO offer_compare (offer_id, compare, compare_key) VALUES \n"
            val insertedOfferCompareValues = offers.mapIndexed { index, offer ->
                offer.compare.map { "( ${insertedOfferIds[index]}, '${safe(it.value)}', '${safe(it.key)}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferCompareValues.isNotEmpty()) {
                val offerCompareQuery = insertCompare + insertedOfferCompareValues
                entityManager.createNativeQuery(offerCompareQuery).executeUpdate()
            }

            val insertRules = "INSERT INTO offer_rules (offer_id, rules, rules_key) VALUES \n"
            val insertedOfferRulesValues = offers.mapIndexed { index, offer ->
                offer.rules.map { "( ${insertedOfferIds[index]}, ${it.value.ordinal}, '${safe(it.key)}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferRulesValues.isNotEmpty()) {
                val insertOfferRulesQuery = insertRules + insertedOfferRulesValues
                entityManager.createNativeQuery(insertOfferRulesQuery).executeUpdate()
            }

            // for proper return data purpose only
            val ids = insertedOfferIds.joinToString(", ")
            val query = "SELECT * FROM offer WHERE offer.id IN ($ids)"
            @Suppress("UNCHECKED_CAST")
            val wrongSortedResult = entityManager.createNativeQuery(query, Offer::class.java).resultList as List<Offer>
            val wrongSortedResultAsMap = wrongSortedResult.map { it.id to it }.toMap()
            result = insertedOfferIds.map { wrongSortedResultAsMap[it] ?: error("was not find bt ids") }
        }
        return syncElementCollections(result)
    }

    @Transactional
    fun updateAll(offers: List<Offer>): List<Offer> {
        var result: List<Offer> = listOf()
        val values = offers.map {
            val isoPattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            val updatedAt = SimpleDateFormat(isoPattern).format(it.updatedAt)
            val createdAt = SimpleDateFormat(isoPattern).format(it.createdAt)
            val id = if (it.id == 0L) "nextval('offer_id_seq')" else it.id.toString()

            "($id, " +
                "'${safe(it.title)}', '${safe(it.description)}', '${safe(it.owner)}', " +
                "'${it.imageUrl}', '${it.worth}', '$createdAt', '$updatedAt')"
        }
        if (values.isNotEmpty()) {
            val insertOffers = "INSERT INTO offer " +
                "(id, title, description, owner, image_url, worth, created_at, updated_at) VALUES \n"
            val insertOffersQuery = insertOffers + values.joinToString(",\n") +
                "\n ON CONFLICT (id) DO UPDATE SET\n" +
                "    title = EXCLUDED.title,\n" +
                "    description = EXCLUDED.description,\n" +
                "    owner = EXCLUDED.owner,\n" +
                "    image_url = EXCLUDED.image_url,\n" +
                "    worth = EXCLUDED.worth,\n" +
                "    updated_at = EXCLUDED.updated_at\n" +
                "RETURNING id;"

            @Suppress("UNCHECKED_CAST")
            val insertedOfferIds: List<Long> = entityManager
                .createNativeQuery(insertOffersQuery)
                .resultList as List<Long>

            // cleanup tags for the all offers in the request
            val offerInByIds = insertedOfferIds.joinToString(", ")
            val cleanUpTagsQuery = "DELETE FROM offer_tags\n" +
                "WHERE offer_tags.offer_id IN ($offerInByIds);"
            entityManager.createNativeQuery(cleanUpTagsQuery).executeUpdate()

            val insertTags = "INSERT INTO offer_tags (offer_id, tags, tags_key) VALUES \n"
            val insertedOfferTagsValues = offers.mapIndexed { index, offer ->
                offer.tags.map { "( ${insertedOfferIds[index]}, " +
                    "'${it.value.replace("'", "''")}', " +
                    "'${it.key.replace("'", "''")}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferTagsValues.isNotEmpty()) {
                val ifConflictPartTagsQuery = "\nON CONFLICT ON CONSTRAINT offer_tags_pkey DO UPDATE SET\n" +
                    "    tags_key = EXCLUDED.tags_key,\n" +
                    "    tags = EXCLUDED.tags"
                val insertOfferTagsQuery = insertTags + insertedOfferTagsValues + ifConflictPartTagsQuery
                entityManager.createNativeQuery(insertOfferTagsQuery).executeUpdate()
            }

            // cleanup compare for the all offers in the request
            val cleanUpCompareQuery = "DELETE FROM offer_compare\n" +
                "WHERE offer_compare.offer_id IN ($offerInByIds);"
            entityManager.createNativeQuery(cleanUpCompareQuery).executeUpdate()

            val insertCompare = "INSERT INTO offer_compare (offer_id, compare, compare_key) VALUES \n"
            val insertedOfferCompareValues = offers.mapIndexed { index, offer ->
                offer.compare.map { "( ${insertedOfferIds[index]}, " +
                    "'${it.value.replace("'", "''")}', " +
                    "'${it.key.replace("'", "''")}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferCompareValues.isNotEmpty()) {
                val ifConflictOfferCompare = "\n ON CONFLICT ON CONSTRAINT offer_compare_pkey " +
                    "DO UPDATE SET\n" +
                    "    compare = EXCLUDED.compare,\n" +
                    "    compare_key = EXCLUDED.compare_key"
                val offerCompareQuery = insertCompare + insertedOfferCompareValues + ifConflictOfferCompare
                entityManager.createNativeQuery(offerCompareQuery).executeUpdate()
            }

            // cleanup rules for the all offers in the request
            val cleanUpRulesQuery = "DELETE FROM offer_rules\n" +
                "WHERE offer_rules.offer_id IN ($offerInByIds);"
            entityManager.createNativeQuery(cleanUpRulesQuery).executeUpdate()

            val insertRules = "INSERT INTO offer_rules (offer_id, rules, rules_key) VALUES \n"
            val insertedOfferRulesValues = offers.mapIndexed { index, offer ->
                offer.rules.map { "( ${insertedOfferIds[index]}, ${it.value.ordinal}, " +
                    "'${it.key.replace("'", "''")}' )" }
            }.flatten().joinToString(",\n")
            if (insertedOfferRulesValues.isNotEmpty()) {
                val ifConflictOfferRules = "\n ON CONFLICT ON CONSTRAINT offer_rules_pkey " +
                    "DO UPDATE SET\n" +
                    "    rules = EXCLUDED.rules,\n" +
                    "    rules_key = EXCLUDED.rules_key"
                val insertOfferRulesQuery = insertRules + insertedOfferRulesValues + ifConflictOfferRules
                entityManager.createNativeQuery(insertOfferRulesQuery).executeUpdate()
            }

            // for proper return data purpose only
            val ids = insertedOfferIds.joinToString(", ")
            val query = "SELECT * FROM offer WHERE offer.id IN ($ids)"
            @Suppress("UNCHECKED_CAST")
            val wrongSortedResult = entityManager.createNativeQuery(query, Offer::class.java).resultList as List<Offer>
            val wrongSortedResultAsMap = wrongSortedResult.map { it.id to it }.toMap()
            result = insertedOfferIds.map { wrongSortedResultAsMap[it] ?: error("was not find bt ids") }
        }
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
            Offer.OfferType.PRODUCT ->
                repository.getAllOffersExceptProductsSlice(pageable.changeOrderFieldsToSnakeCase())
            else -> repository.getAllOffersBy(pageable.changeOrderFieldsToCamelCase())
        }

        return syncElementCollections(
            slice,
            syncCompare,
            syncRules,
            syncPrices
        )
    }

    override fun findAllWithoutOwner(): List<Offer> {
        return repository.findAllWithoutOwner()
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

        Logger.debug("syncElementCollections get raw result objects: $step1", LoggerType.PROFILING)

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
        Logger.debug("syncElementCollections mapping ids: $step2", LoggerType.PROFILING)

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

        Logger.debug("syncElementCollections merge result: $mergeResult", LoggerType.PROFILING)

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
        Logger.debug("syncElementCollections syncPriceRules: $loadPrices", LoggerType.PROFILING)

        return result
    }

    private fun safe(unsafe: String): String {
        return unsafe.replace("'", "''")
    }
 }
