package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class SearchRequestService(
    private val repository: RepositoryStrategy<SearchRequestRepository>,
    private val querySearchRequestCrudRepository: QuerySearchRequestCrudRepository
) {

    fun putSearchRequest(
        id: Long,
        owner: String,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return supplyAsyncEx(Supplier {
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

            repository.changeStrategy(strategy).saveSearchRequest(updateSearchRequest)
        })
    }

    fun deleteSearchRequest(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            val deletedId = repository.changeStrategy(strategy).deleteSearchRequest(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }

            deletedId
        })
    }

    fun deleteSearchRequests(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return runAsyncEx(Runnable {
            repository.changeStrategy(strategy).deleteSearchRequests(owner)
        })
    }

    fun deleteQuerySearchRequest(owner: String): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            querySearchRequestCrudRepository.deleteAllByOwner(owner)
        })
    }

    fun getSearchRequests(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return supplyAsyncEx(Supplier {
            val repository = repository.changeStrategy(strategy)

            when {
                id > 0 -> {
                    val searchRequest = repository.findByIdAndOwner(id, owner)

                    if (searchRequest != null) {
                        return@Supplier arrayListOf(searchRequest)
                    }
                    return@Supplier emptyList<SearchRequest>()
                }
                owner != "0x0" -> return@Supplier repository.findByOwner(owner)
                else -> return@Supplier repository.findAll()
            }
        })
    }

    fun cloneSearchRequestWithOfferSearches(
        owner: String,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return supplyAsyncEx(Supplier {
            val clonedSearchRequest = SearchRequest(
                searchRequest.id,
                owner,
                searchRequest.tags
            )

            repository.changeStrategy(strategy).cloneSearchRequestWithOfferSearches(clonedSearchRequest)
        })
    }

    fun getPageableRequests(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<SearchRequest>> {

        return supplyAsyncEx(Supplier {
            val repository = repository.changeStrategy(strategy)
            return@Supplier repository.findAll(page)
        })
    }

    fun getSearchRequestTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy).getTotalCount()
        })
    }

    fun getRequestByOwnerAndTag(
        owner: String,
        tagKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return supplyAsyncEx(Supplier {
            val repository = repository.changeStrategy(strategy)
            return@Supplier repository.getRequestByOwnerAndTag(owner, tagKey)
        })
    }
}
