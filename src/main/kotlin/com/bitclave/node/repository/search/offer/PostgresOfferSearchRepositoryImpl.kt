package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferSearch
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
    val repository: OfferSearchCrudRepository
) : OfferSearchRepository {

    private val logger = KotlinLogging.logger {}

    override fun deleteAllBySearchRequestId(id: Long): Long = repository.deleteAllBySearchRequestId(id)

    override fun deleteAllBySearchRequestIdIn(ids: List<Long>): Long = repository.deleteAllBySearchRequestIdIn(ids)

    override fun deleteAllByOwner(owner: String): List<Long> {
        return repository.deleteAllByOwner(owner)
    }

    override fun deleteAllByOfferIdAndStateIn(offerId: Long): Int {
        return repository.deleteAllByOfferIdAndStateIn(offerId)
    }

    override fun deleteAllByOfferId(id: Long): Long = repository.deleteAllByOfferId(id)

    override fun save(list: List<OfferSearch>): List<OfferSearch> =
        repository.save(list).toList()

    override fun save(item: OfferSearch) {
        save(listOf(item))
    }

    override fun findById(id: Long): OfferSearch? {
        return repository.findOne(id)
    }

    override fun findById(ids: List<Long>): List<OfferSearch> {
        return repository.findAll(ids)
            .toList()
    }

    override fun findBySearchRequestId(id: Long): List<OfferSearch> {
        return repository.findBySearchRequestId(id)
    }

    override fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch> {
        return repository.findBySearchRequestId(id, pageable)
    }

    override fun findBySearchRequestIdInAndOwner(ids: List<Long>, owner: String): List<OfferSearch> {
        return repository.findBySearchRequestIdInAndOwner(ids, owner)
    }

    override fun findByOfferId(id: Long): List<OfferSearch> {
        return repository.findByOfferId(id)
    }

    override fun findBySearchRequestIdAndOfferId(searchRequestId: Long, offerId: Long): List<OfferSearch> {
        return repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
    }

    override fun findBySearchRequestIdAndOfferIds(searchRequestId: Long, offerIds: List<Long>): List<OfferSearch> {
        return repository.findBySearchRequestIdAndOfferIdIn(searchRequestId, offerIds)
    }

    override fun findByOwner(owner: String, sort: Sort?): List<OfferSearch> {
        return when (sort) {
            Sort(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndSortByRank(owner)
            Sort(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndSortByUpdatedAt(owner)
            Sort(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndSortByOfferPriceWorth(owner)
            Sort(Sort.Direction.ASC, "cashback") ->
                repository.getOfferSearchByOwnersAndSortByCashBack(owner)
            else ->
                repository.findByOwner(owner)
        }
    }

    override fun findAllByOwnerAndStateIn(
        owner: String,
        state: List<OfferAction>,
        sort: Sort?
    ): List<OfferSearch> {
        val condition = state.map { it.ordinal.toLong() }
        return when (sort) {
            Sort(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndStateSortByRank(owner, condition)
            Sort(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndStateSortByUpdatedAt(owner, condition)
            Sort(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndStateAndSortByOfferPriceWorth(owner, condition)
            Sort(Sort.Direction.ASC, "cashback") ->
                repository.getOfferSearchByOwnerAndStateAndSortByCashBack(owner, condition)
            else ->
                repository.findAllByOwnerAndStateIn(owner, condition)
        }
    }

    override fun findAllByOwnerAndSearchRequestIdIn(
        owner: String,
        searchRequestIds: List<Long>,
        sort: Sort?
    ): List<OfferSearch> {
        var result = listOf<OfferSearch>()

        when (sort) {
            Sort(Sort.Direction.ASC, "rank") -> {
                val timeMs = measureTimeMillis {
                    result = repository.getOfferSearchByOwnerAndSearchRequestIdInSortByRank(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort ByRank ms: $timeMs, size: ${result.size}" }
            }

            Sort(Sort.Direction.ASC, "updatedAt") -> {
                val timeMs = measureTimeMillis {
                    result = repository
                        .getOfferSearchByOwnerAndSearchRequestIdInSortByUpdatedAt(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort by UpdateAt ms: $timeMs" }
            }
            Sort(Sort.Direction.ASC, "price") -> {
                val timeMs = measureTimeMillis {
                    result = repository
                        .getOfferSearchByOwnerAndSearchRequestIdInAndSortByOfferPriceWorth(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort by cashback ms: $timeMs" }
            }
            Sort(Sort.Direction.ASC, "cashback") -> {
                result = repository.getOfferSearchByOwnerAndSearchRequestIdInAndSortByCashback(owner, searchRequestIds)
            }

            else -> {
                val timeMs = measureTimeMillis {
                    result = repository.findAllByOwnerAndSearchRequestIdIn(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, default sorting ms: $timeMs" }
            }
        }
        return result
    }

    override fun findAllByOwnerAndStateAndSearchRequestIdIn(
        owner: String,
        searchRequestIds: List<Long>,
        state: List<OfferAction>,
        sort: Sort?
    ): List<OfferSearch> {
        val conditions = state.map { it.ordinal.toLong() }
        return when (sort) {
            Sort(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByRank(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByUpdatedAt(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByOfferPriceWorth(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort(Sort.Direction.ASC, "cashback") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdAndStateSortByCashback(
                    owner,
                    searchRequestIds,
                    conditions
                )
            else ->
                repository.findByOwnerAndSearchRequestIdInAndStateIn(owner, searchRequestIds, conditions)
        }
    }

    override fun findByOwnerAndOfferId(owner: String, offerId: Long): List<OfferSearch> {
        // val searchRequestList = searchRequestRepository.findByOwner(owner)
        // val searchRequestIDs = searchRequestList.map { it.id }.toSet()

        // val offerSearchList = repository.findByOfferId(offerId)

        // return offerSearchList.filter { searchRequestIDs.contains(it.searchRequestId) }
        return repository.findByOwnerAndOfferId(owner, offerId)
    }

    override fun findByOwnerAndOfferIdIn(owner: String, offerIds: List<Long>): List<OfferSearch> {
        return repository.findByOwnerAndOfferIdIn(owner, offerIds)
    }

    override fun findAll(pageable: Pageable): Page<OfferSearch> {
        return repository.findAll(pageable)
    }

    override fun findAll(): List<OfferSearch> {
        return repository.findAll()
            .asSequence()
            .toList()
    }

    override fun findAllDiff(): List<OfferSearch> {
        return repository.findAllDiff()
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun countBySearchRequestId(id: Long): Long = repository.countBySearchRequestId(id)
}
