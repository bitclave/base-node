package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*

open class OfferSearchEvent (
        _updater: String,
        _status: OfferResultAction
){
    private var date: Date = Date();
    private var updater: String = _updater;
    private var status: OfferResultAction = _status;
}

class OfferSearchEventConfirmed  (
        _updater: String,
        _status: OfferResultAction,
        _CAT: String
) : OfferSearchEvent (_updater, _status) {
    private var CAT: String = _CAT;
}

@Service
@Qualifier("v1")
class OfferSearchService(
        private val searchRequestRepository: RepositoryStrategy<SearchRequestRepository>,
        private val offerRepository: RepositoryStrategy<OfferRepository>,
        private val offerSearchRepository: RepositoryStrategy<OfferSearchRepository>
) {
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

            val result = if (searchRequestId != null) {
                repository.findBySearchRequestId(searchRequestId)

            } else {
                val offerSearch: OfferSearch? = repository.findById(offerSearchId!!)
                if (offerSearch != null) arrayListOf(offerSearch) else emptyList<OfferSearch>()
            }

            val ids: Map<Long, OfferSearch> = result.associate { Pair(it.offerId, it) }

            val offers = offerRepository.changeStrategy(strategy)
                    .findById(ids.keys.toList())

            offers.filter { ids.containsKey(it.id) }
                    .map { OfferSearchResultItem(ids[it.id]!!, it) }
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
            var info: String = "[]";
            if (!offerSearch.info.isEmpty()) {
                info = "[\"" + offerSearch.info + "\"]";
            }

            offerSearchRepository.changeStrategy(strategy)
                    .saveSearchResult(OfferSearch(
                            0,
                            offerSearch.searchRequestId,
                            offerSearch.offerId,
                            OfferResultAction.NONE,
                            offerSearch.lastUpdated,
//                            offerSearch.lastUpdate,
                            info,
                            offerSearch.events
                    ))
        }
    }

    private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create();

    fun addEventTo(
            event: String,
            offerSearchId: Long,
            strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId) ?: throw BadArgumentException("offer search item id not exist")

            item.events.add(event)
            item.lastUpdated = Date().toString()

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
                    ?: throw AccessDeniedException()

            item.state = OfferResultAction.COMPLAIN
            repository.saveSearchResult(item)

            var event: OfferSearchEvent = OfferSearchEvent(callerPublicKey, item.state);
            addEventTo(GSON.toJson(event), offerSearchId, strategy);
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
            repository.saveSearchResult(item)

            var event: OfferSearchEvent = OfferSearchEvent(callerPublicKey, item.state);
            addEventTo(GSON.toJson(event), offerSearchId, strategy);
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
                    ?: throw AccessDeniedException()

            item.state = OfferResultAction.REJECT
            repository.saveSearchResult(item)

            var event: OfferSearchEvent = OfferSearchEvent(callerPublicKey, item.state);
            addEventTo(GSON.toJson(event), offerSearchId, strategy);
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
                    ?: throw AccessDeniedException()

            item.state = OfferResultAction.CLAIMPURCHASE
            repository.saveSearchResult(item)

            var event: OfferSearchEvent = OfferSearchEvent(callerPublicKey, item.state);
            addEventTo(GSON.toJson(event), offerSearchId, strategy);
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
                    ?: throw BadArgumentException("offer search item id not exist: " + offerSearchId.toString())

            System.out.println("postback confirmation: found offerSearchId with offerId="
                    + item.offerId.toString()
                    + " requestId="
                    + item.searchRequestId.toString());


            // check requestId exist
            var request: SearchRequest = searchRequestRepository.changeStrategy(strategy)
                    .findById(item.searchRequestId)
                    ?: throw BadArgumentException( "searchRequestId does not exists: " + item.searchRequestId.toString());

            // check OfferId exist
            val offer: Offer = offerRepository.changeStrategy(strategy)
                    .findById(item.offerId)
                    ?: throw BadArgumentException( "offerId does not exists: " + item.offerId.toString());

            // check that the owner is the caller
            if (offer.owner != callerPublicKey)
                throw BadArgumentException("the caller must be the owner of the offer")

//            if (request.owner != userBaseId)
//                throw AccessDeniedException()

            item.state = OfferResultAction.CONFIRMED
            repository.saveSearchResult(item)

            var event: OfferSearchEventConfirmed = OfferSearchEventConfirmed(callerPublicKey, item.state, "22");
            addEventTo(GSON.toJson(event), offerSearchId, strategy);
        }
    }

    fun getOfferSearches(
            strategy: RepositoryStrategyType,
            offerId: Long,
            searchRequestId: Long? = null
    ): CompletableFuture<List<OfferSearch>> {

        return CompletableFuture.supplyAsync {

            val repository = offerSearchRepository.changeStrategy(strategy)

            if(searchRequestId != null)
                return@supplyAsync repository.findBySearchRequestIdAndOfferId(searchRequestId, offerId)
            else
                return@supplyAsync repository.findByOfferId(offerId)

        }
    }
}
