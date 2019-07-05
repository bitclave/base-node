package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class SearchRequestService(
    private val repository: RepositoryStrategy<SearchRequestRepository>,
    private val repositoryOfferSearch: RepositoryStrategy<OfferSearchRepository>,
    private val querySearchRequestCrudRepository: QuerySearchRequestCrudRepository,
    private val offerSearchService: OfferSearchService
) {

    fun putSearchRequest(
        id: Long,
        owner: String,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return CompletableFuture.supplyAsync {
            var existedSearchRequest: SearchRequest? = null

            if (id > 0) {
                existedSearchRequest = repository.changeStrategy(strategy)
                    .findByIdAndOwner(id, owner) ?: throw BadArgumentException()
            }

            val createAt = existedSearchRequest?.createdAt ?: Date()
            val updateSearchRequest = SearchRequest(
                id,
                owner,
                searchRequest.tags,
                createAt
            )

            offerSearchService.deleteBySearchRequestId(id, owner, strategy)

            repository
                .changeStrategy(strategy)
                .save(updateSearchRequest)
        }
    }

    fun deleteSearchRequest(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync {
            val deletedId = repository.changeStrategy(strategy).deleteByIdAndOwner(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }

            offerSearchService.deleteBySearchRequestId(id, owner, strategy)

            return@supplyAsync deletedId
        }
    }

    fun deleteSearchRequests(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync {
            val deletedIds = repository
                .changeStrategy(strategy)
                .findByOwner(owner)
                .map { it.id }
            repository.changeStrategy(strategy).deleteByOwner(owner)

            offerSearchService.deleteBySearchRequestIdIn(deletedIds, owner, strategy)
        }
    }

    fun deleteQuerySearchRequest(owner: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            querySearchRequestCrudRepository.deleteAllByOwner(owner)
        }
    }

    fun getSearchRequests(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return CompletableFuture.supplyAsync {
            val repository = repository.changeStrategy(strategy)

            when {
                id > 0 -> {
                    val searchRequest = repository.findByIdAndOwner(id, owner)

                    if (searchRequest != null) {
                        return@supplyAsync arrayListOf(searchRequest)
                    }
                    return@supplyAsync emptyList<SearchRequest>()
                }
                owner != "0x0" -> return@supplyAsync repository.findByOwner(owner)
                else -> return@supplyAsync repository.findAll()
            }
        }
    }

    fun cloneSearchRequestWithOfferSearches(
        owner: String,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return CompletableFuture.supplyAsync {

            val existingRequest = repository
                .changeStrategy(strategy)
                .findById(searchRequest.id)
                ?: throw BadArgumentException("SearchRequest does not exist: ${searchRequest.id}")

            val createSearchRequest = repository
                .changeStrategy(strategy)
                .save(SearchRequest(0, owner, existingRequest.tags.toMap()))

            offerSearchService.cloneOfferSearchOfSearchRequest(owner, existingRequest.id, createSearchRequest, strategy)
                .get()

            createSearchRequest
        }
    }

    fun getPageableRequests(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<SearchRequest>> {

        return CompletableFuture.supplyAsync {
            val repository = repository.changeStrategy(strategy)
            return@supplyAsync repository.findAll(page)
        }
    }

    fun getSearchRequestTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync {

            val repository = repository.changeStrategy(strategy)

            return@supplyAsync repository.getTotalCount()
        }
    }

    fun getRequestByOwnerAndTag(
        owner: String,
        tagKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return CompletableFuture.supplyAsync {
            val repository = repository.changeStrategy(strategy)
            return@supplyAsync repository.getRequestByOwnerAndTag(owner, tagKey)
        }
    }
}
