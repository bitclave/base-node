package com.bitclave.node.services.v1

import com.bitclave.node.BaseNodeApplication
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
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

            repository.changeStrategy(strategy).saveSearchRequest(updateSearchRequest)
        }
    }

    fun deleteSearchRequest(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync {
            val deletedId = repository.changeStrategy(strategy).deleteSearchRequest(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }

            return@supplyAsync deletedId
        }
    }

    fun deleteSearchRequests(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync {
            repository.changeStrategy(strategy).deleteSearchRequests(owner)
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

        return CompletableFuture.supplyAsync(Supplier {
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
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun cloneSearchRequestWithOfferSearches(
        owner: String,
        searchRequest: SearchRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return CompletableFuture.supplyAsync(Supplier {
            val clonedSearchRequest = SearchRequest(
                searchRequest.id,
                owner,
                searchRequest.tags
            )

            repository.changeStrategy(strategy).cloneSearchRequestWithOfferSearches(clonedSearchRequest)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getPageableRequests(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<SearchRequest>> {

        return CompletableFuture.supplyAsync(Supplier {
            val repository = repository.changeStrategy(strategy)
            return@Supplier repository.findAll(page)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
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

        return CompletableFuture.supplyAsync(Supplier {
            val repository = repository.changeStrategy(strategy)
            return@Supplier repository.getRequestByOwnerAndTag(owner, tagKey)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }
}
