package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchResultItem
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class OfferSearchService(
        private val searchRequestRepository: RepositoryStrategy<SearchRequestRepository>,
        private val offerRepository: RepositoryStrategy<OfferRepository>,
        private val offerSearchRepository: RepositoryStrategy<OfferSearchRepository>
) {
    fun getOffersResult(
            clientId: String,
            strategy: RepositoryStrategyType,
            searchRequestId: Long? = null,
            searchResultId: Long? = null
    ): CompletableFuture<List<OfferSearchResultItem>> {

        return CompletableFuture.supplyAsync({
            if (searchRequestId == null && searchResultId == null) {
                throw BadArgumentException("specify parameter searchRequestId or searchResultId")
            }

            val repository = offerSearchRepository.changeStrategy(strategy)

            val result = if (searchRequestId != null) {
                repository.findBySearchRequestId(searchRequestId)

            } else {
                val offerSearch: OfferSearch? = repository.findById(searchResultId!!)
                if (offerSearch != null) arrayListOf(offerSearch) else emptyList<OfferSearch>()
            }

            val ids: Map<Long, OfferSearch> = result.associate { Pair(it.offerId, it) }

            val offers = offerRepository.changeStrategy(strategy)
                    .findById(ids.keys.toList())

            offers.filter { ids.containsKey(it.id) }
                    .map { OfferSearchResultItem(ids[it.id]!!, it) }
        })
    }

    fun saveOfferSearch(
            clientId: String,
            offerSearch: OfferSearch,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            searchRequestRepository.changeStrategy(strategy)
                    .findByIdAndOwner(offerSearch.searchRequestId, clientId)
                    ?: throw BadArgumentException("search request id not exist")

            offerRepository.changeStrategy(strategy)
                    .findById(offerSearch.offerId)
                    ?: throw BadArgumentException("offer id not exist")

            offerSearchRepository.changeStrategy(strategy)
                    .saveSearchResult(OfferSearch(
                            0,
                            offerSearch.searchRequestId,
                            offerSearch.offerId,
                            OfferResultAction.NONE
                    ))
        })
    }

    fun complain(
            clientId: String,
            offerSearchId: Long,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val repository = offerSearchRepository.changeStrategy(strategy)
            val item = repository.findById(offerSearchId)
                    ?: throw BadArgumentException("offer search item id not exist")

            searchRequestRepository.changeStrategy(strategy)
                    .findByIdAndOwner(item.searchRequestId, clientId)
                    ?: AccessDeniedException()

            item.state = OfferResultAction.REJECT;
            repository.saveSearchResult(item)
        })
    }

}
