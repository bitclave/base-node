package com.bitclave.node.services.v1

// import com.appoptics.metrics.client.Tag
import com.bitclave.node.configuration.properties.AppOpticsProperties
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.entities.SearchRequest
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.query.QuerySearchRequestCrudRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
// import com.bitclave.node.utils.AppOpticsUtil
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import kotlin.system.measureTimeMillis

@Service
@Qualifier("v1")
class SearchRequestService(
    private val repository: RepositoryStrategy<SearchRequestRepository>,
    private val querySearchRequestCrudRepository: QuerySearchRequestCrudRepository,
    private val offerSearchService: OfferSearchService,
    appOpticsProperties: AppOpticsProperties
) {

    private val logger = KotlinLogging.logger {}

    private val appOpticsUtil = AppOpticsUtil(appOpticsProperties)

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

            offerSearchService.deleteBySearchRequestId(id, owner, strategy)

            repository
                .changeStrategy(strategy)
                .save(updateSearchRequest)
        })
    }

    fun putSearchRequests(
        owner: String,
        searchRequests: List<SearchRequest>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return supplyAsyncEx(Supplier {
            val existedSearchRequests = repository.changeStrategy(strategy)
                .findById(searchRequests.map { it.id }.distinct())

            existedSearchRequests.forEach {
                if (it.owner != owner) {
                    throw AccessDeniedException("search request id: ${it.id} has different owner")
                }
            }

            val grouped = existedSearchRequests.groupBy { it.id }

            val result = searchRequests
                .map {
                    grouped[it.id]?.get(0)?.copy(tags = it.tags, updatedAt = Date())
                        ?: SearchRequest(0, owner, it.tags)
                }

            offerSearchService.deleteBySearchRequestIdIn(
                existedSearchRequests.map { it.id }.distinct(),
                owner,
                strategy
            )

            repository
                .changeStrategy(strategy)
                .save(result)
        })
    }

    fun deleteSearchRequest(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            val deletedId = repository.changeStrategy(strategy).deleteByIdAndOwner(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }

            offerSearchService.deleteBySearchRequestId(id, owner, strategy)

            deletedId
        })
    }

    fun deleteSearchRequests(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return runAsyncEx(Runnable {
            val deletedIds = repository
                .changeStrategy(strategy)
                .findByOwner(owner)
                .map { it.id }
            repository.changeStrategy(strategy).deleteByOwner(owner)

            offerSearchService.deleteBySearchRequestIdIn(deletedIds, owner, strategy)
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
        searchRequestIds: List<Long>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {

        return supplyAsyncEx(Supplier {

            var existingRequest = emptyList<SearchRequest>()
            val step1 = measureTimeMillis {
                existingRequest = repository
                    .changeStrategy(strategy)
                    .findById(searchRequestIds)
            }
            // logger.debug { "clone search request step1: $step1" }
            // appOpticsUtil.sendToAppOptics(
            //     "com.bitclave.node.services.v1.cloneSearchRequestWithOfferSearches.step1",
            //     (step1).toDouble(),
            //     Tag("owner", owner)
            // )

            var preparedRequests = emptyList<SearchRequest>()

            val step2 = measureTimeMillis {
                val notExisted = existingRequest.filter { !searchRequestIds.contains(it.id) }

                if (notExisted.isNotEmpty()) {
                    throw BadArgumentException("SearchRequest does not exist: $notExisted")
                }

                preparedRequests = existingRequest.map { SearchRequest(0, owner, it.tags.toMap()) }
            }
            // logger.debug { "clone search request step2: $step2" }
            // appOpticsUtil.sendToAppOptics(
            //     "com.bitclave.node.services.v1.cloneSearchRequestWithOfferSearches.step2",
            //     (step2).toDouble(),
            //     Tag("owner", owner)
            // )

            var createSearchRequests = emptyList<SearchRequest>()

            val step3 = measureTimeMillis {
                createSearchRequests = repository
                    .changeStrategy(strategy)
                    .save(preparedRequests)
            }

            // logger.debug { "clone search request step3: $step3" }
            // appOpticsUtil.sendToAppOptics(
            //     "com.bitclave.node.services.v1.cloneSearchRequestWithOfferSearches.step3",
            //     (step3).toDouble(),
            //     Tag("owner", owner)
            // )

            val zipped = existingRequest.map { it.id }
                .zip(createSearchRequests.map { it.id })

            val step4 = measureTimeMillis {
                try {
                    offerSearchService.cloneOfferSearchOfSearchRequest(
                        owner,
                        zipped,
                        strategy
                    ).get()
                } catch (e: Throwable) {
                    repository.changeStrategy(strategy)
                        .deleteByIdIn(createSearchRequests.map { it.id })

                    throw e
                }
            }
            // logger.debug { "clone search request step4 (full clone offer search): $step4" }
            // appOpticsUtil.sendToAppOptics(
            //     "com.bitclave.node.services.v1.cloneSearchRequestWithOfferSearches.step4",
            //     (step4).toDouble(),
            //     Tag("owner", owner)
            // )
            // appOpticsUtil.sendToAppOptics(
            //     "com.bitclave.node.services.v1.cloneSearchRequestWithOfferSearches.total",
            //     (step1 + step2 + step3 + step4).toDouble(),
            //     Tag("owner", owner)
            // )
            createSearchRequests
        })
    }

    fun getPageableRequests(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<SearchRequest>> {
        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy).findAll(page)
        })
    }

    fun getRequestsSlice(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Slice<SearchRequest>> {
        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy).findAllSlice(page)
        })
    }

    fun getRequestsSliceByOwners(
        owners: List<String>,
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Slice<SearchRequest>> {
        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy).findByOwnerInSlice(owners, page)
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
            repository.changeStrategy(strategy).getRequestByOwnerAndTag(owner, tagKey)
        })
    }

    fun getSearchRequestWithSameTags(
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<SearchRequest>> {
        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy).getSearchRequestWithSameTags()
        })
    }
}
