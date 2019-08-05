package com.bitclave.node.services.v1

import com.appoptics.metrics.client.Tag
import com.bitclave.node.configuration.properties.AppOpticsProperties
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferAction
import com.bitclave.node.repository.models.OfferInteraction
import com.bitclave.node.repository.models.OfferInteractionId
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.QuerySearchRequest
import com.bitclave.node.repository.models.controllers.EnrichedOffersWithCountersResponse
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.rtSearch.RtSearchRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.interaction.OfferInteractionRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.AppOpticsUtil
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import kotlin.math.min
import kotlin.system.measureTimeMillis

data class OfferSearchEvent(
    val updater: String,
    val status: OfferAction,
    val date: Date = Date()
)

data class OfferSearchEventConfirmed(
    val updater: String,
    val status: OfferAction,
    val CAT: String,
    val date: Date = Date()
)

@Service
@Qualifier("v1")
class OfferSearchService(
    private val searchRequestRepository: RepositoryStrategy<SearchRequestRepository>,
    private val offerRepository: RepositoryStrategy<OfferRepository>,
    private val offerSearchRepository: RepositoryStrategy<OfferSearchRepository>,
    private val querySearchRequestCrudRepository: QuerySearchRequestCrudRepository,
    private val rtSearchRepository: RtSearchRepository,
    private val offerInteractionRepository: RepositoryStrategy<OfferInteractionRepository>,
    private val gson: Gson,
    appOpticsProperties: AppOpticsProperties
) {

    private val defaultUnsignedOwner = "0fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"

    private val logger = KotlinLogging.logger {}

    private val appOpticsUtil = AppOpticsUtil(appOpticsProperties)

    fun getOffersResult(
        strategy: RepositoryStrategyType,
        searchRequestId: Long = 0,
        offerSearchId: Long = 0,
        pageRequest: PageRequest = PageRequest.of(0, 20)
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return supplyAsyncEx(Supplier {
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

                val pageable = PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize)
                PageImpl(arrayOfferSearch, pageable, arrayOfferSearch.size.toLong())
            }

            val content = offerSearchListToResult(
                result.content,
                offerRepository.changeStrategy(strategy),
                offerInteractionRepository.changeStrategy(strategy)
            )

            val pageable = PageRequest.of(result.number, result.size, result.sort)

            PageImpl(content, pageable, result.totalElements) as Page<OfferSearchResultItem>
        })
    }

    fun getOffersAndOfferSearchesByParams(
        strategy: RepositoryStrategyType,
        owner: String,
        unique: Boolean = false,
        searchRequestIds: List<Long> = emptyList(),
        state: List<OfferAction> = emptyList(),
        pageRequest: PageRequest = PageRequest.of(0, 20),
        interaction: Boolean = false
    ): CompletableFuture<Page<OfferSearchResultItem>> {

        return supplyAsyncEx(Supplier {
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
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.step1",
                (step1).toDouble(),
                Tag("owner", owner)
            )

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
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.step2",
                (step2).toDouble(),
                Tag("owner", owner)
            )

            var subItems = listOf<OfferSearch>()
            val step3 = measureTimeMillis {
                subItems = filteredByUnique.subList(
                    min(pageRequest.pageNumber * pageRequest.pageSize, filteredByUnique.size),
                    min((pageRequest.pageNumber + 1) * pageRequest.pageSize, filteredByUnique.size)
                )
            }
            logger.debug { "3 step) subItems ms: $step3" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.step3",
                (step3).toDouble(),
                Tag("owner", owner)
            )

            var content = listOf<OfferSearchResultItem>()
            val step4 = measureTimeMillis {
                content = offerSearchListToResult(
                    subItems,
                    offerRepository.changeStrategy(strategy),
                    offerInteractionRepository.changeStrategy(strategy),
                    interaction
                )
            }
            logger.debug { "4 step) content ms: $step4" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.step4",
                (step4).toDouble(),
                Tag("owner", owner)
            )

            var pageImpl = PageImpl(listOf<OfferSearchResultItem>())
            val step5 = measureTimeMillis {
                val pageable = PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize)
                pageImpl = PageImpl(content, pageable, filteredByUnique.size.toLong())
            }
            logger.debug { "5 step) content ms: $step5" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.step5",
                (step5).toDouble(),
                Tag("owner", owner)
            )
            logger.debug { "total) ms: ${step1 + step2 + step3 + step4 + step5}" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.getOffersAndOfferSearchesByParams.total",
                (step1 + step2 + step3 + step4 + step5).toDouble(),
                Tag("owner", owner)
            )
            pageImpl as Page<OfferSearchResultItem>
        })
    }

    fun saveNewOfferSearch(
        offerSearch: OfferSearch,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val searchRequest = searchRequestRepository.changeStrategy(strategy)
                .findById(offerSearch.searchRequestId)
                ?: throw BadArgumentException("search request id not exist")

            offerRepository.changeStrategy(strategy)
                .findById(offerSearch.offerId)
                ?: throw BadArgumentException("offer id not exist")

            logger.debug {
                "saveNewOfferSearch: " +
                    "$searchRequest, $offerSearch"
            }

            offerSearchRepository.changeStrategy(strategy)
                .save(
                    OfferSearch(
                        0,
                        searchRequest.owner,
                        offerSearch.searchRequestId,
                        offerSearch.offerId
                    )
                )

            offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(offerSearch.offerId, searchRequest.owner)
                ?: offerInteractionRepository
                    .changeStrategy(strategy).save(
                        OfferInteraction(
                            0,
                            searchRequest.owner,
                            offerSearch.offerId,
                            OfferAction.NONE
                        )
                    )
        })
    }

    fun addEventTo(
        event: String,
        offerSearchId: Long,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val item = offerSearchRepository
                .changeStrategy(strategy)
                .findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(event)

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(updatedAt = Date(), events = events))
        })
    }

    fun complain(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findByIdAndOwner(item.searchRequestId, callerPublicKey)
                ?: throw BadArgumentException("searchRequestId id not exist")

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            val event = gson.toJson(OfferSearchEvent(callerPublicKey, state.state))

            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(event)

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferAction.COMPLAIN, updatedAt = Date(), events = events))
        })
    }

    fun evaluate(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findByIdAndOwner(item.searchRequestId, callerPublicKey)
                ?: throw AccessDeniedException()

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, state.state)
            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(gson.toJson(event))

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferAction.EVALUATE, updatedAt = Date(), events = events))
        })
    }

    fun reject(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findByIdAndOwner(item.searchRequestId, callerPublicKey)
                ?: throw BadArgumentException("searchRequestId item id not exist")

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, state.state)
            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(gson.toJson(event))

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferAction.REJECT, updatedAt = Date(), events = events))
        })
    }

    fun claimPurchase(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

//            searchRequestRepository.changeStrategy(strategy)
//                .findByIdAndOwner(item.searchRequestId, callerPublicKey)
//                ?: throw BadArgumentException("searchRequestId id not exist")

            // while the correct logic is above and we need to verify that the caller
            // is the owner of the offerSearch, we have some workaround on shepherd-backend
            // that fails when we add this verification. SHEP-558 created to track the removal of the
            // hack
            searchRequestRepository.changeStrategy(strategy)
                .findById(item.searchRequestId)
                ?: throw BadArgumentException("searchRequestId id not exist")

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            val event = OfferSearchEvent(callerPublicKey, state.state)
            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(gson.toJson(event))

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferAction.CLAIMPURCHASE, updatedAt = Date(), events = events))
        })
    }

    fun confirm(
        offerSearchId: Long,
        callerPublicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
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

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(item.offerId, item.owner)
                ?: OfferInteraction(0, item.owner, item.offerId)

            // fixme anti-pattern 'magic numbers'
            val event = OfferSearchEventConfirmed(callerPublicKey, state.state, "22")
            val events = mutableListOf<String>()
            events.addAll(state.events)
            events.add(gson.toJson(event))

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferAction.CONFIRMED, updatedAt = Date(), events = events))
        })
    }

    fun getOfferSearches(
        strategy: RepositoryStrategyType,
        offerId: Long,
        searchRequestId: Long? = null
    ): CompletableFuture<List<OfferSearch>> {

        return supplyAsyncEx(Supplier {

            val repository = offerSearchRepository.changeStrategy(strategy)

            if (searchRequestId != null)
                repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
            else
                repository.findByOfferId(offerId)
        })
    }

    fun getPageableOfferSearches(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<OfferSearch>> {
        return supplyAsyncEx(Supplier {
            offerSearchRepository.changeStrategy(strategy).findAll(page)
        })
    }

    fun getConsumersOfferSearches(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Slice<OfferSearch>> {
        return supplyAsyncEx(Supplier {
            offerSearchRepository.changeStrategy(strategy).findAllSlice(page)
        })
    }

    fun getConsumersOfferSearchesBySearchRequestIds(
        ids: List<Long>,
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Slice<OfferSearch>> {
        return supplyAsyncEx(Supplier {
            offerSearchRepository.changeStrategy(strategy).findBySearchRequestIdInSlice(ids, page)
        })
    }

    fun getDanglingOfferSearches(
        strategy: RepositoryStrategyType,
        type: Int
    ): CompletableFuture<List<OfferSearch>> {

        return supplyAsyncEx(Supplier {
            when (type) {
                0 -> {
                    // have no matching offer
                    offerSearchRepository.changeStrategy(strategy).findAllWithoutOffer()
                }
                1 -> {
                    // have no matching searchRequest
                    offerSearchRepository.changeStrategy(strategy).findAllWithoutSearchRequest()
                }
                2 -> {
                    // have no matching owner
                    offerSearchRepository.changeStrategy(strategy).findAllWithoutOwner()
                }
                3 -> {
                    // have no offer_interaction with matching offer_id and owner
                    offerSearchRepository.changeStrategy(strategy).findAllWithoutOfferInteraction()
                }
                else -> throw BadArgumentException("specify search type")
            }
        })
    }

    fun getDiffOfferSearches(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return supplyAsyncEx(Supplier {
            offerSearchRepository.changeStrategy(strategy)
                .findAllDiff()
        })
    }

    fun getOfferSearchesByIds(
        strategy: RepositoryStrategyType,
        ids: List<Long>
    ): CompletableFuture<List<OfferSearch>> {

        return supplyAsyncEx(Supplier {
            offerSearchRepository
                .changeStrategy(strategy)
                .findById(ids)
        })
    }

    fun getOfferSearchTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            offerSearchRepository.changeStrategy(strategy).getTotalCount()
        })
    }

    fun getOfferSearchCountBySearchRequestIds(
        searchRequestIds: List<Long>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Map<Long, Long>> {

        return supplyAsyncEx(Supplier {
            val uniqueRequestIds = searchRequestIds.distinct()

            val result = mutableMapOf<Long, Long>()
            val repository = offerSearchRepository.changeStrategy(strategy)

            uniqueRequestIds.forEach {
                result[it] = repository.countBySearchRequestId(it)
            }

            result.toMap()
        })
    }

    fun cloneOfferSearchOfSearchRequest(
        owner: String,
        originToCopySearchRequestIds: List<Pair<Long, Long>>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return supplyAsyncEx(Supplier {
            val pairs = originToCopySearchRequestIds.unzip()
            val searchRequestsIds = pairs.first.toMutableList()
            searchRequestsIds.addAll(pairs.second)

            val step1 = measureTimeMillis {
                val existedSearchRequests = searchRequestRepository.changeStrategy(strategy)
                    .findById(searchRequestsIds.distinct())

                if (existedSearchRequests.size != searchRequestsIds.distinct().size) {
                    throw BadArgumentException("some search request id not found")
                }

                existedSearchRequests.forEach {
                    if (pairs.second.contains(it.id) && it.owner != owner) {
                        throw BadArgumentException("invalid search request id or owner")
                    }
                }
            }
            logger.debug { "clone offer search step1: $step1" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step1",
                (step1).toDouble(),
                Tag("owner", owner)
            )

            var allOfferSearches = emptyList<OfferSearch>()
            val repository = offerSearchRepository.changeStrategy(strategy)

            val step2 = measureTimeMillis {
                allOfferSearches = repository.findBySearchRequestIdIn(searchRequestsIds.distinct())
            }
            logger.debug { "clone offer search step2: $step2" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step2",
                (step2).toDouble(),
                Tag("owner", owner)
            )

            var result = emptyList<OfferSearch>()

            val step3 = measureTimeMillis {
                result = originToCopySearchRequestIds.map { pair ->
                    val forCopy = allOfferSearches.filter { it.searchRequestId == pair.first }
                    val existed = allOfferSearches
                        .filter { it.searchRequestId == pair.second }
                        .groupBy { it.hashCodeByOfferIdAndOwner() }

                    val filterExcludeExist = forCopy
                        .filter { !existed.containsKey(it.hashCodeByOfferIdAndOwner()) }
                        .map { OfferSearch(0, owner, pair.second, it.offerId) }

                    filterExcludeExist
                }.flatten()
            }
            logger.debug { "clone offer search step3: $step3" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step3",
                (step3).toDouble(),
                Tag("owner", owner)
            )

            var interactions = emptyList<OfferInteraction>()

            val step4 = measureTimeMillis {
                val offerIds = result.map { it.offerId }.distinct()
                val existedOffersInInteractions = offerInteractionRepository.changeStrategy(strategy)
                    .findByOfferIdInAndOwner(offerIds, owner)
                    .map { it.offerId }
                    .toSet()
                val notExistedOffersInInteractions = offerIds.filter { !existedOffersInInteractions.contains(it) }
                interactions = notExistedOffersInInteractions.map { OfferInteraction(0, owner, it) }
            }
            logger.debug { "clone offer search step4: $step4" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step4",
                (step4).toDouble(),
                Tag("owner", owner)
            )

            val step5 = measureTimeMillis {
                offerInteractionRepository.changeStrategy(strategy).save(interactions)
            }
            logger.debug { "clone offer search step5: $step5, count ${interactions.size}" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step5",
                (step5).toDouble(),
                Tag("owner", owner)
            )

            var savedResult = emptyList<OfferSearch>()
            val step6 = measureTimeMillis {
                savedResult = repository.save(result)
            }
            logger.debug { "clone offer search step6: $step6, count ${result.size}" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.step6",
                (step6).toDouble(),
                Tag("owner", owner)
            )
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.cloneOfferSearchOfSearchRequest.total",
                (step1 + step2 + step3 + step4 + step5 + step6).toDouble(),
                Tag("owner", owner)
            )

            savedResult
        })
    }

    fun getSuggestion(decodedQuery: String, size: Int): CompletableFuture<List<String>> {
        return supplyAsyncEx(Supplier {
            var result = emptyList<String>()

            try {
                result = rtSearchRepository
                    .getSuggestionByQuery(decodedQuery, size)
                    .get()
            } catch (e: Throwable) {
                logger.error("rt-search getSuggestion error: $e")

                if (e.cause is HttpServerErrorException &&
                    (e.cause as HttpServerErrorException).rawStatusCode <= 499
                ) {
                    throw e
                }
            }

            result
        })
    }

    fun createOfferSearchesByQuery(
        searchRequestId: Long,
        owner: String,
        query: String,
        pageRequest: PageRequest,
        strategyType: RepositoryStrategyType,
        filters: Map<String, List<String>>? = mapOf(),
        mode: String? = ""
    ): CompletableFuture<EnrichedOffersWithCountersResponse> {
        return supplyAsyncEx(Supplier {
            val searchRequest = searchRequestRepository
                .changeStrategy(strategyType)
                .findById(searchRequestId)
                ?: throw NotFoundException("search request not found by id: $searchRequestId")

            if (searchRequest.tags.keys.indexOf("rtSearch") <= -1) {
                throw BadArgumentException("SearchRequest not has rtSearch tag")
            }

            val step1 = measureTimeMillis {
                deleteBySearchRequestId(searchRequest.id, owner, strategyType)

                searchRequestRepository
                    .changeStrategy(strategyType)
                    .save(searchRequest.copy(updatedAt = Date()))
            }
            logger.debug { "step 1 -> save(). ms: $step1" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step1",
                (step1).toDouble(),
                Tag("owner", owner)
            )

            val querySearchRequest = QuerySearchRequest(0, owner, query)

            val step2 = measureTimeMillis {
                querySearchRequestCrudRepository.save(querySearchRequest)
            }
            logger.debug { "step 2 -> querySearchRequestCrudRepository.save(). ms: $step2" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step2",
                (step2).toDouble(),
                Tag("owner", owner)
            )

            var existedOfferSearches: List<OfferSearch> = emptyList()

            val step3 = measureTimeMillis {
                existedOfferSearches = offerSearchRepository
                    .changeStrategy(strategyType)
                    .findBySearchRequestId(searchRequestId)
            }
            logger.debug { "step 3 -> findBySearchRequestId(). ms: $step3" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step3",
                (step3).toDouble(),
                Tag("owner", owner)
            )

            var offerIds: Page<Long> = PageImpl(emptyList<Long>(), PageRequest.of(0, 1), 0)
            var counters: Map<String, Map<String, Int>> = mapOf()

            val step4 = measureTimeMillis {
                try {
                    val searchedData = rtSearchRepository.getOffersIdByQuery(query, pageRequest, filters, mode).get()
                    offerIds = searchedData.getPageableOfferIds()
                    counters = searchedData.counters
                } catch (e: Throwable) {
                    logger.error("rt-search error: $e")

                    if (e.cause is HttpServerErrorException &&
                        (e.cause as HttpServerErrorException).rawStatusCode > 499
                    ) {
                        val pageable = PageRequest.of(0, 1)
                        return@Supplier EnrichedOffersWithCountersResponse(
                            PageImpl(emptyList<OfferSearchResultItem>(), pageable, 0),
                            counters
                        )
                    } else {
                        throw e
                    }
                }
            }
            logger.debug { "step 4 -> getOffersIdByQuery(). ms: $step4" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step4",
                (step4).toDouble(),
                Tag("owner", owner)
            )

            var offerSearches: List<OfferSearch> = emptyList()
            val step5 = measureTimeMillis {
                val setOfExistedOfferSearch = existedOfferSearches
                    .map { it.offerId }
                    .toSet()

                val offerIdsWithoutExisted = offerIds
                    .filter { !setOfExistedOfferSearch.contains(it) }
                    .toList()

                offerSearches = offerIdsWithoutExisted.map {
                    OfferSearch(0, owner, searchRequest.id, it)
                }
            }
            logger.debug { "step 5 -> merge offerSearches. ms: $step5" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step5",
                (step5).toDouble(),
                Tag("owner", owner)
            )

            val step6 = measureTimeMillis {
                offerSearchRepository
                    .changeStrategy(strategyType)
                    .save(offerSearches)

                val uniqueOwners = offerSearches.map { it.owner }
                val uniqueOfferIds = offerSearches.map { it.offerId }

                val states = offerInteractionRepository
                    .changeStrategy(strategyType)
                    .findByOfferIdInAndOwnerIn(uniqueOfferIds, uniqueOwners)
                    .groupBy { OfferInteractionId(it.offerId, it.owner) }

                val stateForSave = offerSearches.filter { states[OfferInteractionId(it.offerId, it.owner)] == null }
                    .map { OfferInteraction(0, it.owner, it.offerId) }

                offerInteractionRepository
                    .changeStrategy(strategyType)
                    .save(stateForSave)
            }
            logger.debug { "step 6 -> save(). ms: $step6" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step6",
                (step6).toDouble(),
                Tag("owner", owner)
            )

            var offerSearchResult: List<OfferSearch> = emptyList()

            val step7 = measureTimeMillis {
                offerSearchResult = offerSearchRepository.changeStrategy(strategyType)
                    .findBySearchRequestIdAndOfferIds(searchRequestId, offerIds.content)
                    .sortedWith(Comparator { a, b ->
                        offerIds.indexOf(a.offerId) - offerIds.indexOf(b.offerId)
                    })
            }
            logger.debug { "step 7 -> findBySearchRequestIdAndOfferIds(). ms: $step7" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step7",
                (step7).toDouble(),
                Tag("owner", owner)
            )

            var result: Page<OfferSearchResultItem> =
                PageImpl(emptyList<OfferSearchResultItem>(), PageRequest.of(0, 1), 0)

            val step8 = measureTimeMillis {
                val resultItems = offerSearchListToResult(
                    offerSearchResult,
                    offerRepository.changeStrategy(strategyType),
                    offerInteractionRepository.changeStrategy(strategyType)
                )

                val pageable = PageRequest.of(offerIds.number, offerIds.size, offerIds.sort)

                result = PageImpl(resultItems, pageable, offerIds.totalElements)
            }
            logger.debug { "step 8 -> findBySearchRequestIdAndOfferIds(). ms: $step8" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.step8",
                (step8).toDouble(),
                Tag("owner", owner)
            )
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.createOfferSearchesByQuery.total",
                (step1 + step2 + step3 + step4 + step5 + step6 + step7 + step8).toDouble(),
                Tag("owner", owner)
            )

            val data = EnrichedOffersWithCountersResponse(result, counters)
            data
        })
    }

    fun getOfferSearchesByQuery(
        query: String,
        pageRequest: PageRequest,
        strategyType: RepositoryStrategyType,
        filters: Map<String, List<String>>? = mapOf(),
        mode: String? = ""
    ): CompletableFuture<EnrichedOffersWithCountersResponse> {
        return supplyAsyncEx(Supplier {
            val offerIds: Page<Long>
            val counters: Map<String, Map<String, Int>>

            try {
                val searchedData = rtSearchRepository.getOffersIdByQuery(query, pageRequest, filters, mode).get()
                offerIds = searchedData.getPageableOfferIds()
                counters = searchedData.counters
            } catch (e: Throwable) {
                logger.error("rt-search error: $e")

                if (e.cause is HttpServerErrorException &&
                    (e.cause as HttpServerErrorException).rawStatusCode > 499
                ) {
                    val pageable = PageRequest.of(0, 1)
                    return@Supplier EnrichedOffersWithCountersResponse(
                        PageImpl(emptyList<OfferSearchResultItem>(), pageable, 0),
                        emptyMap()
                    )
                } else {
                    throw e
                }
            }

            val offers = offerRepository
                .changeStrategy(strategyType)
                .findByIds(offerIds.content.distinct())

            val resultItems = offers.map {
                OfferSearchResultItem(
                    OfferSearch(0, defaultUnsignedOwner, it.id),
                    it,
                    OfferInteraction(0, defaultUnsignedOwner, it.id)
                )
            }

            val pageable = PageRequest.of(offerIds.number, offerIds.size, offerIds.sort)
            val result = PageImpl(resultItems, pageable, offerIds.totalElements)
            val data = EnrichedOffersWithCountersResponse(result, counters)
            data
        })
    }

    fun getInteractions(
        owner: String,
        states: List<OfferAction> = emptyList(),
        offers: List<Long> = emptyList(),
        strategy: RepositoryStrategyType = RepositoryStrategyType.POSTGRES
    ): CompletableFuture<List<OfferInteraction>> {
        return supplyAsyncEx(Supplier {
            val repos = offerInteractionRepository.changeStrategy(strategy)

            when {
                states.isNotEmpty() && offers.isNotEmpty() ->
                    repos.findByOwnerAndOfferIdInAndStateIn(owner, offers, states)

                states.isEmpty() && offers.isNotEmpty() -> repos.findByOfferIdInAndOwner(offers, owner)

                states.isNotEmpty() && offers.isEmpty() -> repos.findByOwnerAndStateIn(owner, states)

                else -> repos.findByOwner(owner)
            }
        })
    }

    fun getDanglingOfferInteractions(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferInteraction>> {

        return supplyAsyncEx(Supplier {
            offerInteractionRepository.changeStrategy(strategy).getDanglingOfferInteractions()
        })
    }

    fun fixDanglingOfferSearchesByCreatingInteractions(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferInteraction>> {
        return supplyAsyncEx(Supplier {
            var danglingOfferSearches = offerSearchRepository.changeStrategy(strategy).findAllWithoutOfferInteraction()
            danglingOfferSearches = danglingOfferSearches.distinctBy { Pair(it.owner, it.offerId) }
            val interactions = danglingOfferSearches.map { OfferInteraction(0, it.owner, it.offerId) }
            offerInteractionRepository.changeStrategy(strategy).save(interactions)
        })
    }

    private fun offerSearchListToResult(
        offerSearch: List<OfferSearch>,
        offersRepository: OfferRepository,
        offerInteractionRepository: OfferInteractionRepository,
        interactions: Boolean = false
    ): List<OfferSearchResultItem> {

        var offerIds = listOf<Long>()
        val step31 = measureTimeMillis {
            offerIds = offerSearch
                .map { it.offerId }
                .distinct()
        }
        logger.debug { "3.1 step) offer Ids ms: $step31" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.offerSearchListToResult.step3.1",
            (step31).toDouble(),
            Tag("empty", "empty")
        )

        var offers = mapOf<Long, List<Offer>>()
        val step32 = measureTimeMillis {

            var ones = listOf<Offer>()
            val step321 = measureTimeMillis {
                ones = offersRepository.findByIds(offerIds)
            }
            logger.debug { "3.2.1 step) findByIds ms: $step321" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.offerSearchListToResult.step3.2.1",
                (step321).toDouble(),
                Tag("empty", "empty")
            )

            val step322 = measureTimeMillis {
                offers = ones.groupBy { it.id }
            }
            logger.debug { "3.2.2 step)  groupBy ms: $step322" }
            appOpticsUtil.sendToAppOptics(
                "com.bitclave.node.services.v1.offerSearchListToResult.step3.2.2",
                (step322).toDouble(),
                Tag("empty", "empty")
            )
        }
        logger.debug { "3.2 step) ids is ${offerIds.size} offer MAP<Long, List<Offer>> ms: $step32" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.offerSearchListToResult.step3.2",
            (step32).toDouble(),
            Tag("empty", "empty")
        )

        var withExistedOffers = listOf<OfferSearch>()
        val step33 = measureTimeMillis {
            withExistedOffers = offerSearch.filter { offers.containsKey(it.offerId) }
        }
        logger.debug { "3.3 step) offerSearch with existed offers ms: $step33" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.offerSearchListToResult.step3.3",
            (step33).toDouble(),
            Tag("empty", "empty")
        )

        val states = mutableMapOf<Long, List<OfferInteraction>>()

        if (interactions && withExistedOffers.isNotEmpty()) {
            val uniqueOffersIds = withExistedOffers.map { it.offerId }
            states.putAll(offerInteractionRepository
                .findByOfferIdInAndOwner(uniqueOffersIds, withExistedOffers[0].owner)
                .groupBy { it.offerId })
        }

        var result = listOf<OfferSearchResultItem>()
        val step34 = measureTimeMillis {
            result = withExistedOffers.map {
                OfferSearchResultItem(
                    it,
                    offers.getValue(it.offerId)[0],
                    states[it.offerId]?.get(0)
                )
            }
        }
        logger.debug { "3.4 step) final result ms: $step34" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.offerSearchListToResult.step3.4",
            (step34).toDouble(),
            Tag("empty", "empty")
        )
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.offerSearchListToResult.total",
            (step31 + step32 + step33 + step34).toDouble(),
            Tag("empty", "empty")
        )

        return result
    }

    fun deleteByOwner(
        owner: String,
        strategyType: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            offerSearchRepository.changeStrategy(strategyType).deleteAllByOwner(owner)
            offerInteractionRepository.changeStrategy(strategyType).deleteAllByOwner(owner)
        })
    }

    fun deleteByOfferId(offerId: Long, strategy: RepositoryStrategyType) {
        val step1 = measureTimeMillis {
            offerSearchRepository.changeStrategy(strategy).deleteAllByOfferId(offerId)
        }
        logger.debug { "deleteByOfferId step 1 ->  ms: $step1" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.deleteByOfferId.step1",
            (step1).toDouble(),
            Tag("empty", "empty")
        )

        var filteredStateIds: List<Long> = emptyList()

        val step2 = measureTimeMillis {
            filteredStateIds = offerInteractionRepository.changeStrategy(strategy)
                .findByOfferId(offerId)
                .filter { it.state == OfferAction.NONE || it.state == OfferAction.REJECT }
                .map { it.id }
        }

        logger.debug { "deleteByOfferId step 2 -> ms: $step2 filteredStateIds: ${filteredStateIds.size}" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.deleteByOfferId.step2",
            (step2).toDouble(),
            Tag("empty", "empty")
        )

        val step3 = measureTimeMillis {
            offerInteractionRepository.changeStrategy(strategy).delete(filteredStateIds)
        }

        logger.debug { "deleteByOfferId step 3 -> ms: $step3 filteredStateIds: ${filteredStateIds.size}" }
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.deleteByOfferId.step3",
            (step3).toDouble(),
            Tag("empty", "empty")
        )
        appOpticsUtil.sendToAppOptics(
            "com.bitclave.node.services.v1.deleteByOfferId.total",
            (step1 + step2 + step3).toDouble(),
            Tag("empty", "empty")
        )
    }

    fun deleteBySearchRequestId(searchRequestId: Long, owner: String, strategy: RepositoryStrategyType) {
        deleteBySearchRequestIdIn(listOf(searchRequestId), owner, strategy)
    }

    fun deleteBySearchRequestIdIn(searchRequestIds: List<Long>, owner: String, strategy: RepositoryStrategyType) {
        val offerIds = offerSearchRepository
            .changeStrategy(strategy)
            .findBySearchRequestIdInAndOwner(searchRequestIds, owner)
            .map { it.offerId }
            .distinct()

        offerSearchRepository.changeStrategy(strategy).deleteAllBySearchRequestIdIn(searchRequestIds)

        val excludeOfferIds = offerSearchRepository
            .changeStrategy(strategy)
            .findByOwnerAndOfferIdIn(owner, offerIds).map { it.offerId }

        val notExistedOfferIds = offerIds.filter { !excludeOfferIds.contains(it) }

        val filteredStateIds = offerInteractionRepository
            .changeStrategy(strategy)
            .findByOfferIdInAndOwner(notExistedOfferIds, owner).filter {
                it.state == OfferAction.NONE || it.state == OfferAction.REJECT
            }.map { it.id }

        offerInteractionRepository.changeStrategy(strategy).delete(filteredStateIds)
    }
}
