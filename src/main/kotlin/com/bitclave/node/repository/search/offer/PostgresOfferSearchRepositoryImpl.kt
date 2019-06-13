package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.ArrayList
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresOfferSearchRepositoryImpl(
    val repository: OfferSearchCrudRepository,
    val searchRequestRepository: SearchRequestRepository
) : OfferSearchRepository {

    private val logger = KotlinLogging.logger {}

    override fun deleteAllBySearchRequestId(id: Long): Long {
        return repository.deleteAllBySearchRequestId(id)
    }

    override fun deleteAllByOwner(owner: String): List<Long> {
        return repository.deleteAllByOwner(owner)
    }

    override fun saveSearchResult(list: List<OfferSearch>) {
        var allOffersByOwner: MutableList<OfferSearch> = emptyList<OfferSearch>().toMutableList()

        val step1 = measureTimeMillis {
            val searchRequestsIds = list
                .map { it.searchRequestId }
                .distinct()

            val existedRequests = searchRequestRepository.findById(searchRequestsIds)
            if (searchRequestsIds.size != existedRequests.size) {
                throw throw BadArgumentException("search request id not exist")
            }

            val owners = list
                .map { it.owner }
                .distinct()

            val offers = list
                .map { it.offerId }
                .distinct()

            allOffersByOwner = when {
                list.size == 1 -> repository
                    .findByOwnerAndOfferId(list[0].owner, list[0].offerId)
                    .toMutableList()

                owners.size == 1 -> repository
                    .findByOwnerAndOfferIdIn(owners[0], offers)
                    .toMutableList()

                list.size > 1 -> repository
                    .findByOwnerIn(owners)
                    .toMutableList()

                else -> mutableListOf()
            }
        }
        logger.debug { "saveSearchResult: step 1: ms: $step1, l1: ${list.size}, l2: ${allOffersByOwner.size}" }

        val step2 = measureTimeMillis {
            list.forEach { offer ->
                val relatedOfferSearches = allOffersByOwner
                    .filter {
                        it.id > 0 &&
                            it.offerId == offer.offerId &&
                            it.owner == offer.owner
                    }

                relatedOfferSearches.forEach { it.updatedAt = offer.updatedAt }

                when {
                    offer.id > 0 -> relatedOfferSearches.forEach { related ->
                        related.state = offer.state
                        related.events = offer.events
                        related.info = offer.info
                    }

                    relatedOfferSearches.isNotEmpty() -> {
                        val firstItem = relatedOfferSearches[0]
                        val events = offer.events
                            .toMutableList()
                        events.addAll(firstItem.events)

                        val copiedOffer = offer.copy(
                            state = firstItem.state,
                            events = events,
                            info = firstItem.info
                        )
                        allOffersByOwner.add(copiedOffer)
                    }

                    relatedOfferSearches.isEmpty() -> allOffersByOwner.add(offer.copy())
                }
            }
        }
        logger.debug { "saveSearchResult: step 2: ms: $step2, l1: ${list.size}, l2: ${allOffersByOwner.size}" }

        val step3 = measureTimeMillis {
            repository.save(allOffersByOwner) ?: throw DataNotSavedException()
        }
        logger.debug { "saveSearchResult: step 3: ms: $step3, l1: ${list.size}, l2: ${allOffersByOwner.size}" }
    }

    override fun saveSearchResult(item: OfferSearch) {
        saveSearchResult(listOf(item))
    }

    override fun findById(id: Long): OfferSearch? {
        return repository.findOne(id)
    }

    override fun findById(ids: List<Long>): List<OfferSearch> {
        return repository.findAll(ids)
            .asSequence()
            .toList()
    }

    override fun findBySearchRequestId(id: Long): List<OfferSearch> {
        return repository.findBySearchRequestId(id)
    }

    override fun findBySearchRequestId(id: Long, pageable: Pageable): Page<OfferSearch> {
        return repository.findBySearchRequestId(id, pageable)
    }

    override fun findBySearchRequestIds(ids: List<Long>): List<OfferSearch> {
        return repository.findBySearchRequestIdIn(ids)
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
        state: List<OfferResultAction>,
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
                repository.findAllByOwnerAndStateIn(owner, state)
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
        state: List<OfferResultAction>,
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
                repository.findByOwnerAndSearchRequestIdInAndStateIn(owner, searchRequestIds, state)
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

    override fun cloneOfferSearchOfSearchRequest(
        sourceSearchRequestId: Long,
        targetSearchRequest: SearchRequest
    ): List<OfferSearch> {
        val copiedOfferSearchList = repository.findBySearchRequestId(sourceSearchRequestId)
        val existedOfferSearchList = repository.findBySearchRequestId(targetSearchRequest.id)

        val toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf()
        for (offerSearch: OfferSearch in copiedOfferSearchList) {
            val exist = existedOfferSearchList
                .find { it.offerId == offerSearch.offerId && it.owner == offerSearch.owner }

            if (exist == null) {
                val newOfferSearch = OfferSearch(
                    0,
                    targetSearchRequest.owner,
                    targetSearchRequest.id,
                    offerSearch.offerId,
                    OfferResultAction.NONE,
                    offerSearch.info,
                    ArrayList()
                )
                toBeSavedOfferSearched.add(newOfferSearch)
            }
        }

        return repository.save(toBeSavedOfferSearched).toList()
    }

    override fun countBySearchRequestId(id: Long): Long = repository.countBySearchRequestId(id)

    override fun deleteAllByOfferIdAndStateIn(offerId: Long): Int {
        return repository.deleteAllByOfferIdAndStateIn(offerId)
    }
}
