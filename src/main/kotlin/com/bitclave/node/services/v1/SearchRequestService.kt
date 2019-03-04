package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class SearchRequestService(
        private val repository: RepositoryStrategy<SearchRequestRepository>
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

        return CompletableFuture.supplyAsync({
            val deletedId = repository.changeStrategy(strategy).deleteSearchRequest(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }

            return@supplyAsync deletedId
        })
    }

    fun deleteSearchRequests(
            owner: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync({
            repository.changeStrategy(strategy).deleteSearchRequests(owner)
        })
    }

    fun getSearchRequests(
            id: Long,
            owner: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return CompletableFuture.supplyAsync({
            val repository = repository.changeStrategy(strategy)

            if (id > 0) {
                val searchRequest = repository.findByIdAndOwner(id, owner)

                if (searchRequest != null) {
                    return@supplyAsync arrayListOf(searchRequest)
                }
                return@supplyAsync emptyList<SearchRequest>()

            } else if (owner != "0x0") {
                return@supplyAsync repository.findByOwner(owner)

            } else {
                return@supplyAsync repository.findAll()
            }
        })
    }

    fun cloneSearchRequestWithOfferSearches(
            owner: String,
            searchRequest: SearchRequest,
            strategy: RepositoryStrategyType
    ): CompletableFuture<SearchRequest> {

        return CompletableFuture.supplyAsync({
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

        return CompletableFuture.supplyAsync({
            val repository = repository.changeStrategy(strategy)
            return@supplyAsync repository.getRequestByOwnerAndTag(owner, tagKey)
        })
    }
}
