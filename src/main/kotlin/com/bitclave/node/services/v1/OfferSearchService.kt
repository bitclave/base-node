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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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

    fun saveOfferSearch(
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

            offerSearchRepository.changeStrategy(strategy)
                    .saveSearchResult(OfferSearch(
                            0,
                            offerSearch.searchRequestId,
                            offerSearch.offerId,
                            OfferResultAction.NONE
                    ))
        }
    }

    fun complain(
            offerSearchId: Long,
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
        }
    }

    fun evaluate(
            offerSearchId: Long,
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
        }
    }

    fun reject(
            offerSearchId: Long,
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
        }
    }

    fun confirm(
            offerSearchId: Long,
            publicKey: String,
//            userBaseId: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)

            // offerSearchId exist
            val item = repository.findById(offerSearchId)
                    ?: throw BadArgumentException("offer search item id not exist")

            // check requestId exist
            var request: SearchRequest = searchRequestRepository.changeStrategy(strategy)
                    .findById(item.searchRequestId)
                    ?: throw AccessDeniedException()

            // check OfferId exist
            val offer: Offer = offerRepository.changeStrategy(strategy)
                    .findById(item.offerId)
                    ?: throw AccessDeniedException()

            // check that the owner is the caller
            if (offer.owner != publicKey)
                throw AccessDeniedException()

//            if (request.owner != userBaseId)
//                throw AccessDeniedException()

            item.state = OfferResultAction.CONFIRMED
            repository.saveSearchResult(item)
        }
    }

    fun getPageableOfferSearches(page: PageRequest,
                                 strategy: RepositoryStrategyType
    ): CompletableFuture<Page<OfferSearch>> {
        return CompletableFuture.supplyAsync {
            val repository = offerSearchRepository.changeStrategy(strategy)
            return@supplyAsync repository.findAll(page)
        }
    }
}
