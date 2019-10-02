package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.entities.OfferAction
import com.bitclave.node.repository.entities.OfferSearch
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
    val repository: OfferSearchCrudRepository
) : OfferSearchRepository {

    private val logger = KotlinLogging.logger {}

    override fun deleteAllBySearchRequestId(id: Long): Int = repository.deleteAllBySearchRequestId(id)

    override fun deleteAllByOfferIds(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        return repository.deleteAllByOfferIds(ids)
    }
    override fun deleteAllBySearchRequestIdIn(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        return repository.deleteAllBySearchRequestIdIn(ids)
    }

    override fun deleteAllByOwner(owner: String): Int {
        return repository.deleteAllByOwner(owner)
    }

    override fun deleteAllByOfferId(id: Long): Int = repository.deleteAllByOfferId(id)

    override fun save(list: List<OfferSearch>): List<OfferSearch> =
        repository.saveAll(list).toList()

    override fun save(item: OfferSearch): OfferSearch = repository.save(item)

    override fun findById(id: Long): OfferSearch? {
        return repository.findByIdOrNull(id)
    }

    override fun findById(ids: List<Long>): List<OfferSearch> {
        return repository.findAllById(ids)
            .toList()
    }

    override fun findBySearchRequestId(id: Long): List<OfferSearch> {
        return repository.findBySearchRequestId(id)
    }

    override fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch> {
        return repository.findBySearchRequestId(id, pageable)
    }

    override fun findBySearchRequestIdIn(ids: List<Long>): List<OfferSearch> =
        repository.findBySearchRequestIdIn(ids).toList()

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
            Sort.by(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndSortByRank(owner)
            Sort.by(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndSortByUpdatedAt(owner)
            Sort.by(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndSortByOfferPriceWorth(owner)
            Sort.by(Sort.Direction.ASC, "cashback") ->
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
            Sort.by(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndStateSortByRank(owner, condition)
            Sort.by(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndStateSortByUpdatedAt(owner, condition)
            Sort.by(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndStateAndSortByOfferPriceWorth(owner, condition)
            Sort.by(Sort.Direction.ASC, "cashback") ->
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
            Sort.by(Sort.Direction.ASC, "rank") -> {
                val timeMs = measureTimeMillis {
                    result = repository.getOfferSearchByOwnerAndSearchRequestIdInSortByRank(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort ByRank ms: $timeMs, size: ${result.size}" }
            }

            Sort.by(Sort.Direction.ASC, "updatedAt") -> {
                val timeMs = measureTimeMillis {
                    result = repository
                        .getOfferSearchByOwnerAndSearchRequestIdInSortByUpdatedAt(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort by UpdateAt ms: $timeMs" }
            }
            Sort.by(Sort.Direction.ASC, "price") -> {
                val timeMs = measureTimeMillis {
                    result = repository
                        .getOfferSearchByOwnerAndSearchRequestIdInAndSortByOfferPriceWorth(owner, searchRequestIds)
                }
                logger.debug { " findAllByOwnerAndSearchRequestIdIn, sort by cashback ms: $timeMs" }
            }
            Sort.by(Sort.Direction.ASC, "cashback") -> {
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
            Sort.by(Sort.Direction.ASC, "rank") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByRank(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort.by(Sort.Direction.ASC, "updatedAt") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByUpdatedAt(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort.by(Sort.Direction.ASC, "price") ->
                repository.getOfferSearchByOwnerAndSearchRequestIdInAndStateSortByOfferPriceWorth(
                    owner,
                    searchRequestIds,
                    conditions
                )
            Sort.by(Sort.Direction.ASC, "cashback") ->
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

    override fun findAllSlice(pageable: Pageable): Slice<OfferSearch> = repository.findAllBy(pageable)

    override fun findBySearchRequestIdInSlice(ids: List<Long>, pageable: Pageable): Slice<OfferSearch> =
        repository.findBySearchRequestIdIn(ids, pageable)

    override fun findAllDiff(): List<OfferSearch> {
        return repository.findAllDiff()
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun countBySearchRequestId(id: Long): Long = repository.countBySearchRequestId(id)

    override fun findAllWithoutOffer(): List<OfferSearch> = repository.findAllWithoutOffer()

    override fun findAllWithoutSearchRequest(): List<OfferSearch> = repository.findAllWithoutSearchRequest()

    override fun findAllWithoutOwner(): List<OfferSearch> = repository.findAllWithoutOwner()

    override fun findAllWithoutOfferInteraction(): List<OfferSearch> = repository.findAllWithoutOfferInteraction()
}
