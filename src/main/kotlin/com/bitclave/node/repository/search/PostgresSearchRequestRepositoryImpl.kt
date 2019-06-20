package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchState
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.repository.search.state.OfferSearchStateCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
    val repository: SearchRequestCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository,
    val offerSearchStateCrudRepository: OfferSearchStateCrudRepository
) : SearchRequestRepository {

    private val logger = KotlinLogging.logger {}

    override fun saveSearchRequest(request: SearchRequest): SearchRequest =
        repository.save(request) ?: throw DataNotSavedException()

    override fun deleteSearchRequest(id: Long, owner: String): Long {
        repository.deleteByIdAndOwner(id, owner)
        offerSearchRepository.deleteAllBySearchRequestId(id)

        return id
    }

    override fun deleteSearchRequests(owner: String): Long {
        // TODO delete OfferSearch based on BULK deleted searchRequest
        return repository.deleteByOwner(owner)
    }

    override fun findById(id: Long): SearchRequest? {
        return repository.findOne(id)
    }

    override fun findById(ids: List<Long>): List<SearchRequest> {
        return repository.findAll(ids).toList()
    }

    override fun findByOwner(owner: String): List<SearchRequest> {
        return repository.findByOwner(owner)
    }

    override fun findByIdAndOwner(id: Long, owner: String): SearchRequest? {
        return repository.findByIdAndOwner(id, owner)
    }

    override fun findAll(): List<SearchRequest> {
        return repository.findAll().toList()
    }

    override fun cloneSearchRequestWithOfferSearches(request: SearchRequest): SearchRequest {
        var existingRequest = SearchRequest()

        val step1 = measureTimeMillis {
            existingRequest = repository.findOne(request.id)
                ?: throw BadArgumentException("SearchRequest does not exist: ${request.id}")
        }
        logger.info { "clone search request step1 $step1" }

        var relatedOfferSearches = listOf<OfferSearch>()

        val step2 = measureTimeMillis {
            relatedOfferSearches = offerSearchRepository.findBySearchRequestId(existingRequest.id)
        }
        logger.info { "clone search request step2 $step2" }

        val createSearchRequest = SearchRequest(
            0,
            request.owner,
            request.tags
        )

        val step3 = measureTimeMillis {
            repository.save(createSearchRequest)
        }
        logger.info { "clone search request step3 $step3" }

        val toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf()
        val step4 = measureTimeMillis {
            for (offerSearch: OfferSearch in relatedOfferSearches) {
                val newOfferSearch = OfferSearch(
                    0,
                    createSearchRequest.owner,
                    createSearchRequest.id,
                    offerSearch.offerId
                )
                toBeSavedOfferSearched.add(newOfferSearch)
                val offerSearchState = offerSearchStateCrudRepository
                    .findByOfferIdAndOwner(offerSearch.offerId, createSearchRequest.owner)

                if (offerSearchState == null) {
                    offerSearchStateCrudRepository.save(
                        OfferSearchState(
                            0,
                            createSearchRequest.owner,
                            offerSearch.offerId
                        )
                    )
                }
            }
        }
        logger.info { "clone search request step4 $step4" }

        val step5 = measureTimeMillis {
            offerSearchRepository.save(toBeSavedOfferSearched)
        }
        logger.info { "clone search request step5 $step5. count of offerSearches: ${toBeSavedOfferSearched.size}" }

        return createSearchRequest
    }

    override fun findAll(pageable: Pageable): Page<SearchRequest> {
        return repository.findAll(pageable)
    }

    override fun getTotalCount(): Long {
        return repository.count()
    }

    override fun getRequestByOwnerAndTag(owner: String, tagKey: String): List<SearchRequest> {
        return repository.getRequestByOwnerAndTag(owner, tagKey)
    }
}
