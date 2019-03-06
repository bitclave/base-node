package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.models.QuerySearchRequest
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.rtSearch.RtSearchRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
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
    private val gson: Gson
) {
//    var logger = LoggerFactory.getLogger(OfferSearchService::class.java)
    private val logger = KotlinLogging.logger {}

    fun getOffersResult(
        strategy: RepositoryStrategyType,
        searchRequestId: Long? = null,
        offerSearchId: Long? = null
    ): CompletableFuture<List<OfferSearchResultItem>> {

        return CompletableFuture.supplyAsync {
            if (searchRequestId == null && offerSearchId == null) {
                throw BadArgumentException("specify parameter searchRequestId or offerSearchId")
            }

            val repository = offerSearchRepository.changeStrategy(strategy)

            // "result" is list of OfferSearches with searchRequestId as was requested in API call
            val result = if (searchRequestId != null) {
                repository.findBySearchRequestId(searchRequestId)
            } else {
                val offerSearch: OfferSearch? = repository.findById(offerSearchId!!)
                if (offerSearch != null) arrayListOf(offerSearch) else emptyList<OfferSearch>()
            }

            offerSearchListToResult(result, offerRepository.changeStrategy(strategy))
        }
    }

    fun getOffersAndOfferSearchesByOwnerResult(
        strategy: RepositoryStrategyType,
        owner: String
    ): CompletableFuture<List<OfferSearchResultItem>> {

        return CompletableFuture.supplyAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)

            // get all searchRequests of the user
//            val searchRequestList = searchRequestRepository.changeStrategy(strategy).findByOwner(owner)

            // get all relevant offerSearches of searchRequests
//            val searchRequestIds: List<Long> = searchRequestList.map { it.id }
//            val offerSearches = repository.findBySearchRequestIds(searchRequestIds)

            var result: List<OfferSearchResultItem> = emptyList()
            val fullBlock = measureTimeMillis {
                val offerSearches = repository.findByOwner(owner)
                result = offerSearchListToResult(offerSearches, offerRepository.changeStrategy(strategy))
            }

            logger.debug("measure: getOffersAndOfferSearchesByOwnerResult -> fullBlock: $fullBlock")

            result
        }
    }

    fun saveNewOfferSearch(
        offerSearch: OfferSearch,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            searchRequestRepository.changeStrategy(strategy)
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
                        offerSearch.owner,
                        offerSearch.searchRequestId,
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
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                ?: throw BadArgumentException("offer search item id not exist")

            item.events.add(event)
            item.updatedAt = Date()

//            val infoAsArrayTmp: MutableList<String> = mutableListOf<String>();
//            infoAsArrayTmp.add("test1");
//            item.info = GSON.toJson(infoAsArrayTmp);

//            try {
//                val type = object : TypeToken<MutableList<String>>() {}.type;
//                val infoAsArray = GSON.fromJson<MutableList<String>>(item.info, type);
//                infoAsArray.add(event);
//                item.info = GSON.toJson(infoAsArray);
//            }
//            catch (e: Exception)
//            {
//                System.out.println(e.localizedMessage);
//            }
            repository.saveSearchResult(item)
        }
    }

    fun addEventTo(
        event: String,
        offerSearch: OfferSearch,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)

            offerSearch.events.add(event)
            offerSearch.updatedAt = Date()

            repository.saveSearchResult(offerSearch)
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

            item.state = OfferResultAction.COMPLAIN
            item.updatedAt = Date()

            repository.saveSearchResult(item)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            addEventTo(gson.toJson(event), offerSearchId, strategy).get()
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

            item.state = OfferResultAction.EVALUATE
            item.updatedAt = Date()

            repository.saveSearchResult(item)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            addEventTo(gson.toJson(event), offerSearchId, strategy).get()
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

            item.state = OfferResultAction.REJECT
            item.updatedAt = Date()

            repository.saveSearchResult(item)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            addEventTo(gson.toJson(event), offerSearchId, strategy).get()
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

            item.state = OfferResultAction.CLAIMPURCHASE
            item.updatedAt = Date()

            repository.saveSearchResult(item)

            val event = OfferSearchEvent(callerPublicKey, item.state)
            addEventTo(gson.toJson(event), offerSearchId, strategy).get()
        }
    }

    fun confirm(
        offerSearchId: Long,
        callerPublicKey: String,
//            userBaseId: String,
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

//            if (request.owner != userBaseId)
//                throw AccessDeniedException()

            item.state = OfferResultAction.CONFIRMED
            item.updatedAt = Date()

            repository.saveSearchResult(item)

            val event =
                OfferSearchEventConfirmed(callerPublicKey, item.state, "22") // fixme anti-pattern 'magic numbers'
            addEventTo(gson.toJson(event), offerSearchId, strategy).get()
        }
    }

    fun getOfferSearches(
        strategy: RepositoryStrategyType,
        offerId: Long,
        searchRequestId: Long? = null
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            if (searchRequestId != null)
                return@supplyAsync repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
            else
                return@supplyAsync repository.findByOfferId(offerId)
        }
    }

    fun getPageableOfferSearches(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<OfferSearch>> {
        return CompletableFuture.supplyAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            return@supplyAsync repository.findAll(page)
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

            when {
                byOffer!! -> {
                    // get all relevant offers of offerSearches
                    val offerIds: List<Long> = allOfferSearches.map { it.offerId }
                    val offers = offerRepository.changeStrategy(strategy).findById(offerIds.distinct())
                    val existedOfferIds: List<Long> = offers.map { it.id }
                    return@supplyAsync allOfferSearches.filter { it.offerId !in existedOfferIds }
                }
                bySearchRequest!! -> {
                    // get all relevant searchRequests of offerSearches
                    val searchRequestIds: List<Long> = allOfferSearches.map { it.searchRequestId }
                    val searchRequests = searchRequestRepository.changeStrategy(strategy).findById(searchRequestIds)
                    val existedSearchRequestIds: List<Long> = searchRequests.map { it.id }
                    return@supplyAsync allOfferSearches.filter { it.searchRequestId !in existedSearchRequestIds }
                }
                else -> throw BadArgumentException("specify either byOffer or bySearchRequest")
            }
        }
    }

    fun getDiffOfferSearches(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            return@supplyAsync repository.findAllDiff()
        }
    }

    fun getOfferSearchesByIds(
        strategy: RepositoryStrategyType,
        ids: List<Long>
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            return@supplyAsync repository.findById(ids)
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

    fun cloneOfferSearchOfSearchRequest(
        id: Long,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {
            searchRequestRepository.changeStrategy(strategy).findById(id)
                ?: throw BadArgumentException()
            return@supplyAsync offerSearchRepository.changeStrategy(strategy)
                .cloneOfferSearchOfSearchRequest(id, searchRequest)
        }
    }

    fun createOfferSearchesByQuery(
        searchRequestId: Long,
        owner: String,
        query: String,
        strategyType: RepositoryStrategyType
    ): CompletableFuture<List<OfferSearchResultItem>> {
        return CompletableFuture.supplyAsync {
            var result: List<OfferSearchResultItem> = emptyList()
            val fullBlockMeasure = measureTimeMillis {
                val searchRequest = searchRequestRepository
                    .changeStrategy(strategyType)
                    .findById(searchRequestId)
                    ?: throw NotFoundException("search request not found by id: $searchRequestId")

                if (searchRequest.tags.keys.indexOf("rtSearch") <= -1) {
                    throw BadArgumentException("SearchRequest not has rtSearch tag")
                }

                searchRequestRepository
                    .changeStrategy(strategyType)
                    .saveSearchRequest(searchRequest.copy(updatedAt = Date()))

                val querySearchRequest = QuerySearchRequest(0, owner, query)

                querySearchRequestCrudRepository.save(querySearchRequest)

                val existedOfferSearches = offerSearchRepository
                    .changeStrategy(strategyType)
                    .findBySearchRequestId(searchRequestId)
                    .map { it.offerId }
                    .toSet()

                var offerIds: List<Long> = emptyList()
                val getFromRtSearch = measureTimeMillis {
                    offerIds = rtSearchRepository
                        .getOffersIdByQuery(query)
                        .get()
                }

                logger.debug("measure: createOfferSearchesByQuery -> getFromRtSearch: $getFromRtSearch")

                val offerIdsWithoutExisted = offerIds
                    .filter { !existedOfferSearches.contains(it) }

                val offerSearches = offerIdsWithoutExisted.map {
                    OfferSearch(0, owner, searchRequest.id, it)
                }

                val saveEach = measureTimeMillis {
                    offerSearches.forEach {
                        offerSearchRepository
                            .changeStrategy(strategyType)
                            .saveSearchResult(it)
                    }
                }
                logger.debug("measure: createOfferSearchesByQuery -> saveEach: $saveEach")

                val findForResult = measureTimeMillis {
                    val offerSearchResult = offerSearchRepository.changeStrategy(strategyType)
                        .findBySearchRequestIdAndOfferIds(searchRequestId, offerIds)

                    result = offerSearchListToResult(
                        offerSearchResult, offerRepository.changeStrategy(strategyType)
                    )
                }
                logger.debug("measure: createOfferSearchesByQuery -> findForResult: $findForResult")
            }
            logger.debug("measure: createOfferSearchesByQuery -> fullBlockMeasure: $fullBlockMeasure")

            return@supplyAsync result
        }
    }

    private fun offerSearchListToResult(
        offerSearch: List<OfferSearch>,
        offersRepository: OfferRepository
    ): List<OfferSearchResultItem> {
        var result: List<OfferSearchResultItem> = emptyList()
        val fullBlock = measureTimeMillis {
            val offerIds = offerSearch.map { it.offerId }
                .distinct()
            val offers = offersRepository
                .findById(offerIds)
                .groupBy { it.id }

            val withExistedOffers = offerSearch.filter { offers.containsKey(it.offerId) }
            result = withExistedOffers.map { OfferSearchResultItem(it, offers.getValue(it.offerId)[0]) }
        }
        logger.debug("measure: offerSearchListToResult -> fullBlock: $fullBlock")

        return result
    }
}
