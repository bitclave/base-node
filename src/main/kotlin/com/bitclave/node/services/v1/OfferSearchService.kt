package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.OfferSearchState
import com.bitclave.node.repository.models.QuerySearchRequest
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.rtSearch.RtSearchRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.repository.search.state.OfferSearchStateRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.Date
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

open class OfferSearchEvent(
    _updater: String,
    _status: OfferResultAction
) {
    private var date: Date = Date()
    private var updater: String = _updater
    private var status: OfferResultAction = _status
}

class OfferSearchEventConfirmed(
    _updater: String,
    _status: OfferResultAction,
    _CAT: String
) : OfferSearchEvent(_updater, _status) {
    private var CAT: String = _CAT
}

@Service
@Qualifier("v1")
class OfferSearchService(
    private val searchRequestRepository: RepositoryStrategy<SearchRequestRepository>,
    private val offerRepository: RepositoryStrategy<OfferRepository>,
    private val offerSearchRepository: RepositoryStrategy<OfferSearchRepository>,
    private val querySearchRequestCrudRepository: QuerySearchRequestCrudRepository,
    private val rtSearchRepository: RtSearchRepository,
    private val offerSearchStateRepository: RepositoryStrategy<OfferSearchStateRepository>,
    private val gson: Gson
) {

    private val logger = KotlinLogging.logger {}

    fun getOffersResult(
        strategy: RepositoryStrategyType,
        searchRequestId: Long = 0,
        offerSearchId: Long = 0,
        pageRequest: PageRequest = PageRequest(0, 20)
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return CompletableFuture.supplyAsync {
            if (searchRequestId <= 0 && offerSearchId <= 0) {
                throw BadArgumentException("specify parameter searchRequestId or offerSearchId")
            }

            val repository = offerSearchRepository.changeStrategy(strategy)

            // "result" is list of OfferSearches with searchRequestId as was requested in API call
            val result: Page<OfferSearch> = if (searchRequestId > 0) {
                repository.findBySearchRequestId(searchRequestId, pageRequest)
            } else {

                val offerSearch: OfferSearch? = repository.findById(offerSearchId)
                val arrayOfferSearch = if (offerSearch != null && pageRequest.pageNumber == 0) {
                    arrayListOf(offerSearch)
                } else {
                    emptyList<OfferSearch>()
                }

                val pageable = PageRequest(pageRequest.pageNumber, pageRequest.pageSize)
                PageImpl(arrayOfferSearch, pageable, arrayOfferSearch.size.toLong()) as Page<OfferSearch>
            }

            val content = offerSearchListToResult(
                result.content,
                offerRepository.changeStrategy(strategy),
                offerSearchStateRepository.changeStrategy(strategy)
            )
            val pageable = PageRequest(result.number, result.size, result.sort)

            PageImpl(content, pageable, result.totalElements)
        }
    }

    fun getOffersAndOfferSearchesByParams(
        strategy: RepositoryStrategyType,
        owner: String,
        unique: Boolean = false,
        searchRequestIds: List<Long> = emptyList(),
        state: List<OfferResultAction> = emptyList(),
        pageRequest: PageRequest = PageRequest(0, 20)
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return CompletableFuture.supplyAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            var offerSearches = listOf<OfferSearch>()

            val step1 = measureTimeMillis {
                offerSearches = when {
                    searchRequestIds.isNotEmpty() && state.isNotEmpty() ->
                        repository.findAllByOwnerAndStateAndSearchRequestIdIn(
                            owner, searchRequestIds, state, pageRequest.sort
                        )

                    searchRequestIds.isNotEmpty() && state.isEmpty() ->
                        repository.findAllByOwnerAndSearchRequestIdIn(owner, searchRequestIds, pageRequest.sort)

                    searchRequestIds.isEmpty() && state.isNotEmpty() ->
                        repository.findAllByOwnerAndStateIn(owner, state, pageRequest.sort)

                    else ->
                        repository.findByOwner(owner, pageRequest.sort)
                }
            }
            logger.debug { "1 step) get data from DB ms: $step1" }

            var filteredByUnique = listOf<OfferSearch>()
            val step2 = measureTimeMillis {
                filteredByUnique = if (unique) {
                    offerSearches
                        .groupBy { it.offerId }
                        .values
                        .map { it[0] }
                } else {
                    offerSearches
                }
            }
            logger.debug { "2 step) filtering by unique ms: $step2" }

            var subItems = listOf<OfferSearch>()
            val step3 = measureTimeMillis {
                subItems = filteredByUnique.subList(
                    Math.min(pageRequest.pageNumber * pageRequest.pageSize, filteredByUnique.size),
                    Math.min((pageRequest.pageNumber + 1) * pageRequest.pageSize, filteredByUnique.size)
                )
            }
            logger.debug { "3 step) subItems ms: $step3" }

            var content = listOf<OfferSearchResultItem>()
            val step4 = measureTimeMillis {
                content = offerSearchListToResult(
                    subItems,
                    offerRepository.changeStrategy(strategy),
                    offerSearchStateRepository.changeStrategy(strategy)
                )
            }
            logger.debug { "4 step) content ms: $step4" }

            val pageable = PageRequest(pageRequest.pageNumber, pageRequest.pageSize)
            PageImpl(content, pageable, filteredByUnique.size.toLong())
        }
    }

    fun saveNewOfferSearch(
        offerSearch: OfferSearch,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val searchRequest = searchRequestRepository.changeStrategy(strategy)
                .findById(offerSearch.searchRequestId)
                ?: throw BadArgumentException("search request id not exist")

            offerRepository.changeStrategy(strategy)
                .findById(offerSearch.offerId)
                ?: throw BadArgumentException("offer id not exist")

            // we want to guarantee that info is always represents a serialized array
            var info = "[]"
            if (!offerSearch.info.isEmpty()) {
                info = "[\"" + offerSearch.info + "\"]"
            }

            offerSearchRepository.changeStrategy(strategy)
                .saveSearchResult(
                    OfferSearch(
                        0,
                        searchRequest.owner,
                        offerSearch.searchRequestId,
                        offerSearch.offerId
                    )
                )

            offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(offerSearch.offerId, searchRequest.owner)
                ?: offerSearchStateRepository
                    .changeStrategy(strategy).save(
                        OfferSearchState(
                            0,
                            searchRequest.owner,
                            offerSearch.offerId,
                            OfferResultAction.NONE,
                            info,
                            offerSearch.events
                        )
                    )
        }
    }

    fun addEventTo(
        event: String,
        offerSearchId: Long,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val item = offerSearchRepository
                .changeStrategy(strategy)
                .findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            state.events.add(event)

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(updatedAt = Date()))
        }
    }

    fun complain(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw BadArgumentException("searchRequestId id not exist")

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            state.events.add(gson.toJson(event))

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.COMPLAIN, updatedAt = Date()))
        }
    }

    fun evaluate(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw AccessDeniedException()

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            state.events.add(gson.toJson(event))

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.EVALUATE, updatedAt = Date()))
        }
    }

    fun reject(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw BadArgumentException("searchRequestId item id not exist")

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            state.events.add(gson.toJson(event))

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.REJECT, updatedAt = Date()))
        }
    }

    fun claimPurchase(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw BadArgumentException("searchRequestId id not exist")

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            state.events.add(gson.toJson(event))

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.CLAIMPURCHASE, updatedAt = Date()))
        }
    }

    fun confirm(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)

            // offerSearchId exist
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist: $offerSearchId")

            // check requestId exist
            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw BadArgumentException("searchRequestId does not exists: ${item.searchRequestId}")

            // check OfferId exist
            val offer: Offer = offerRepository.changeStrategy(strategy)
                .findById(item.offerId)
                ?: throw BadArgumentException("offerId does not exists: ${item.offerId}")

            // check that the owner is the caller
            if (offer.owner != callerPublicKey)
                throw BadArgumentException("the caller must be the owner of the offer")

            val state = offerSearchStateRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferSearchState(0, item.owner, item.offerId)

            val event =
                OfferSearchEventConfirmed(callerPublicKey, item.state, "22") // fixme anti-pattern 'magic numbers'
            state.events.add(gson.toJson(event))

            offerSearchStateRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.CONFIRMED, updatedAt = Date()))
        }
    }

    fun getOfferSearches(
        strategy: RepositoryStrategyType,
        offerId: Long,
        searchRequestId: Long? = null
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            val result = if (searchRequestId != null)
                repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
            else
                repository.findByOfferId(offerId)

            this.mergeWithOfferSearchState(
                result,
                offerSearchStateRepository.changeStrategy(strategy)
            )
        }
    }

    fun getPageableOfferSearches(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<OfferSearch>> {
        return CompletableFuture.supplyAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val result = repository.findAll(page)

            val merged = this.mergeWithOfferSearchState(
                result.content,
                offerSearchStateRepository.changeStrategy(strategy)
            )

            result.content.clear()
            result.content.addAll(merged)

            result
        }
    }

    fun getDanglingOfferSearches(
        strategy: RepositoryStrategyType,
        byOffer: Boolean? = false,
        bySearchRequest: Boolean? = false
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            // get all offerSearches
            val allOfferSearches = repository.findAll()

            val result = when {
                byOffer!! -> {
                    // get all relevant offers of offerSearches
                    val offerIds: List<Long> = allOfferSearches.map { it.offerId }
                    val offers = offerRepository.changeStrategy(strategy).findByIds(offerIds.distinct())
                    val existedOfferIds: List<Long> = offers.map { it.id }
                    allOfferSearches.filter { it.offerId !in existedOfferIds }
                }
                bySearchRequest!! -> {
                    // get all relevant searchRequests of offerSearches
                    val searchRequestIds: List<Long> = allOfferSearches.map { it.searchRequestId }
                    val searchRequests = searchRequestRepository.changeStrategy(strategy).findById(searchRequestIds)
                    val existedSearchRequestIds: List<Long> = searchRequests.map { it.id }
                    allOfferSearches.filter { it.searchRequestId !in existedSearchRequestIds }
                }
                else -> throw BadArgumentException("specify either byOffer or bySearchRequest")
            }

            this.mergeWithOfferSearchState(
                result,
                offerSearchStateRepository.changeStrategy(strategy)
            )
        }
    }

    fun getDiffOfferSearches(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            val result = repository.findAllDiff()

            this.mergeWithOfferSearchState(
                result,
                offerSearchStateRepository.changeStrategy(strategy)
            )
        }
    }

    fun getOfferSearchesByIds(
        strategy: RepositoryStrategyType,
        ids: List<Long>
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            val result = repository.findById(ids)

            this.mergeWithOfferSearchState(
                result,
                offerSearchStateRepository.changeStrategy(strategy)
            )
        }
    }

    fun getOfferSearchTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            return@supplyAsync repository.getTotalCount()
        }
    }

    fun getOfferSearchCountBySearchRequestIds(
        searchRequestIds: List<Long>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Map<Long, Long>> {

        return CompletableFuture.supplyAsync {
            val uniqueRequestIds = searchRequestIds.distinct()

            val result = HashMap<Long, Long>()
            val repository = offerSearchRepository.changeStrategy(strategy)

            uniqueRequestIds.forEach {
                result[it] = repository.countBySearchRequestId(it)
            }

            result
        }
    }

    fun cloneOfferSearchOfSearchRequest(
        id: Long,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {
            searchRequestRepository.changeStrategy(strategy).findById(id)
                ?: throw BadArgumentException()

            val result = offerSearchRepository.changeStrategy(strategy)
                .cloneOfferSearchOfSearchRequest(id, searchRequest)

            this.mergeWithOfferSearchState(
                result,
                offerSearchStateRepository.changeStrategy(strategy)
            )
        }
    }

    fun createOfferSearchesByQuery(
        searchRequestId: Long,
        owner: String,
        query: String,
        pageRequest: PageRequest,
        strategyType: RepositoryStrategyType,
        interests: List<String>? = listOf(),
        mode: String? = ""
    ): CompletableFuture<Page<OfferSearchResultItem>> {
        return CompletableFuture.supplyAsync {
            val searchRequest = searchRequestRepository
                .changeStrategy(strategyType)
                .findById(searchRequestId)
                ?: throw NotFoundException("search request not found by id: $searchRequestId")

            if (searchRequest.tags.keys.indexOf("rtSearch") <= -1) {
                throw BadArgumentException("SearchRequest not has rtSearch tag")
            }

            val step1 = measureTimeMillis {
                searchRequestRepository
                    .changeStrategy(strategyType)
                    .saveSearchRequest(searchRequest.copy(updatedAt = Date()))
            }
            logger.debug { "step 1 -> saveSearchRequest(). ms: $step1" }

            val querySearchRequest = QuerySearchRequest(0, owner, query)

            val step2 = measureTimeMillis {
                querySearchRequestCrudRepository.save(querySearchRequest)
            }
            logger.debug { "step 2 -> querySearchRequestCrudRepository.save(). ms: $step2" }

            var existedOfferSearches: List<OfferSearch> = emptyList()

            val step3 = measureTimeMillis {
                existedOfferSearches = offerSearchRepository
                    .changeStrategy(strategyType)
                    .findBySearchRequestId(searchRequestId)
            }
            logger.debug { "step 3 -> findBySearchRequestId(). ms: $step3" }

            var offerIds: Page<Long> = PageImpl(emptyList<Long>(), PageRequest(0, 1), 0)

            val step4 = measureTimeMillis {
                try {
                    offerIds = rtSearchRepository
                        .getOffersIdByQuery(query, pageRequest, interests, mode)
                        .get()
                } catch (e: HttpClientErrorException) {
                    logger.error("rt-search error: $e")

                    if (e.rawStatusCode > 499) {
                        val pageable = PageRequest(0, 1)
                        return@supplyAsync PageImpl(emptyList<OfferSearchResultItem>(), pageable, 0)
                    } else {
                        throw e
                    }
                }
            }
            logger.debug { "step 4 -> getOffersIdByQuery(). ms: $step4" }

            var offerSearches: List<OfferSearch> = emptyList()
            val step5 = measureTimeMillis {
                val setOfExistedOfferSearch = existedOfferSearches
                    .map { it.offerId }
                    .toSet()

                val offerIdsWithoutExisted = offerIds
                    .filter { !setOfExistedOfferSearch.contains(it) }

                offerSearches = offerIdsWithoutExisted.map {
                    OfferSearch(0, owner, searchRequest.id, it)
                }
            }
            logger.debug { "step 5 -> merge offerSearches. ms: $step5" }

            val step6 = measureTimeMillis {
                offerSearchRepository
                    .changeStrategy(strategyType)
                    .saveSearchResult(offerSearches)
            }
            logger.debug { "step 6 -> saveSearchResult(). ms: $step6" }

            var offerSearchResult: List<OfferSearch> = emptyList()

            val step7 = measureTimeMillis {
                offerSearchResult = offerSearchRepository.changeStrategy(strategyType)
                    .findBySearchRequestIdAndOfferIds(searchRequestId, offerIds.content)
                    .sortedWith(Comparator { a, b ->
                        offerIds.indexOf(a.offerId) - offerIds.indexOf(b.offerId)
                    })
            }
            logger.debug { "step 7 -> findBySearchRequestIdAndOfferIds(). ms: $step7" }

            var result: Page<OfferSearchResultItem> = PageImpl(emptyList<OfferSearchResultItem>(), PageRequest(0, 1), 0)

            val step8 = measureTimeMillis {
                val resultItems = offerSearchListToResult(
                    offerSearchResult,
                    offerRepository.changeStrategy(strategyType),
                    offerSearchStateRepository.changeStrategy(strategyType)
                )

                val pageable = PageRequest(offerIds.number, offerIds.size, offerIds.sort)

                result = PageImpl(resultItems, pageable, offerIds.totalElements)
            }
            logger.debug { "step 8 -> findBySearchRequestIdAndOfferIds(). ms: $step8" }

            result
        }
    }

    private fun offerSearchListToResult(
        offerSearch: List<OfferSearch>,
        offersRepository: OfferRepository,
        offesSearchStateRepository: OfferSearchStateRepository
    ): List<OfferSearchResultItem> {

        var offerIds = listOf<Long>()
        val step31 = measureTimeMillis {
            offerIds = offerSearch
                .map { it.offerId }
                .distinct()
        }
        logger.debug { "3.1 step) offer Ids ms: $step31" }

        var offers = mapOf<Long, List<Offer>>()
        val step32 = measureTimeMillis {

            var ones = listOf<Offer>()
            val step321 = measureTimeMillis {
                ones = offersRepository.findByIds(offerIds)
            }
            logger.debug { "3.2.1 step) findByIds ms: $step321" }

            val step322 = measureTimeMillis {
                offers = ones.groupBy { it.id }
            }
            logger.debug { "3.2.2 step)  groupBy ms: $step322" }
        }
        logger.debug { "3.2 step) ids is ${offerIds.size} offer MAP<Long, List<Offer>> ms: $step32" }

        var withExistedOffers = listOf<OfferSearch>()
        val step33 = measureTimeMillis {
            withExistedOffers = this.mergeWithOfferSearchState(
                offerSearch.filter { offers.containsKey(it.offerId) },
                offesSearchStateRepository
            )
        }
        logger.debug { "3.3 step) offerSearch with existed offers ms: $step33" }

        var result = listOf<OfferSearchResultItem>()
        val step34 = measureTimeMillis {
            result = withExistedOffers.map { OfferSearchResultItem(it, offers.getValue(it.offerId)[0]) }
        }
        logger.debug { "3.4 step) final result ms: $step34" }

        return result
    }

    fun mergeWithOfferSearchState(
        offerSearches: List<OfferSearch>,
        offerSearchStateRepository: OfferSearchStateRepository
    ): List<OfferSearch> {
        // todo think how to do it more simple
        var result = offerSearches

        val step = measureTimeMillis {
            val uniqueByOfferIdAndOwner = offerSearches
                .distinctBy { "${it.offerId}${it.owner}" }

            val owners = uniqueByOfferIdAndOwner.map { it.owner }
            val offerIds = uniqueByOfferIdAndOwner.map { it.offerId }

            val states = offerSearchStateRepository
                .findByOfferIdInAndOwnerIn(offerIds, owners)
                .groupBy { "${it.offerId}${it.owner}" }

            result = result.map {
                val offerSearchState = states["${it.offerId}${it.owner}"]?.get(0) ?: OfferSearchState(
                    0,
                    it.owner,
                    it.offerId,
                    OfferResultAction.NONE
                )

                it.copy(
                    state = offerSearchState.state,
                    info = offerSearchState.info,
                    events = offerSearchState.events,
                    updatedAt = offerSearchState.updatedAt
                )
            }
        }
        logger.debug { "mergeWithOfferSearchState final result ms: $step" }

        return result
    }

    fun deleteByOwner(
        owner: String,
        strategyType: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            offerSearchRepository.changeStrategy(strategyType).deleteAllByOwner(owner)
            offerSearchStateRepository.changeStrategy(strategyType).deleteAllByOwner(owner)
        }
    }
}
