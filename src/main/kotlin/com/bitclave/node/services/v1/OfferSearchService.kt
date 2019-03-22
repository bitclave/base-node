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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture

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
                PageImpl(arrayOfferSearch, pageable, arrayOfferSearch.size.toLong())
            }

            val content = offerSearchListToResult(result.content, offerRepository.changeStrategy(strategy))
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

            val offerSearches = when {
                searchRequestIds.isNotEmpty() && state.isEmpty() ->
                    repository.findAllByOwnerAndSearchRequestIdIn(owner, searchRequestIds)

                searchRequestIds.isEmpty() && state.isNotEmpty() -> repository.findAllByOwnerAndStateIn(owner, state)

                else -> repository.findByOwner(owner)
            }

            val filteredByUnique = if (unique) {
                offerSearches.groupBy { it.offerId }
                    .values.map { it[0] }
            } else {
                offerSearches
            }

            val subItems = filteredByUnique.subList(
                Math.min(pageRequest.pageNumber * pageRequest.pageSize, filteredByUnique.size),
                Math.min((pageRequest.pageNumber + 1) * pageRequest.pageSize, filteredByUnique.size)
            )

            val content = offerSearchListToResult(subItems, offerRepository.changeStrategy(strategy))

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
                    val offers = offerRepository.changeStrategy(strategy).findByIds(offerIds.distinct())
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
            return@supplyAsync offerSearchRepository.changeStrategy(strategy)
                .cloneOfferSearchOfSearchRequest(id, searchRequest)
        }
    }

    fun createOfferSearchesByQuery(
        searchRequestId: Long,
        owner: String,
        query: String,
        pageRequest: PageRequest,
        strategyType: RepositoryStrategyType
    ): CompletableFuture<Page<OfferSearchResultItem>> {
        return CompletableFuture.supplyAsync {
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

            val offerIds = rtSearchRepository
                .getOffersIdByQuery(query, pageRequest)
                .get()

            val setOfExistedOfferSearch = existedOfferSearches
                .map { it.offerId }
                .toSet()

            val offerIdsWithoutExisted = offerIds
                .filter { !setOfExistedOfferSearch.contains(it) }

            val offerSearches = offerIdsWithoutExisted.map {
                OfferSearch(0, owner, searchRequest.id, it)
            }

            offerSearchRepository
                .changeStrategy(strategyType)
                .saveSearchResult(offerSearches)

            val offerSearchResult = offerSearchRepository.changeStrategy(strategyType)
                .findBySearchRequestIdAndOfferIds(searchRequestId, offerIds.content)

            val resultItems = offerSearchListToResult(
                offerSearchResult, offerRepository.changeStrategy(strategyType)
            )

            val pageable = PageRequest(offerIds.number, offerIds.size, offerIds.sort)

            PageImpl(resultItems, pageable, offerIds.totalElements) as Page<OfferSearchResultItem>
        }
    }

    private fun offerSearchListToResult(
        offerSearch: List<OfferSearch>,
        offersRepository: OfferRepository
    ): List<OfferSearchResultItem> {
        val offerIds = offerSearch.map { it.offerId }
            .distinct()
        val offers = offersRepository
            .findByIds(offerIds)
            .groupBy { it.id }

        val withExistedOffers = offerSearch.filter { offers.containsKey(it.offerId) }
        return withExistedOffers.map { OfferSearchResultItem(it, offers.getValue(it.offerId)[0]) }
    }
}
