package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.ArrayList
import kotlin.system.measureTimeMillis

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
    val repository: SearchRequestCrudRepository,
    val offerSearchRepository: OfferSearchCrudRepository
) : SearchRequestRepository {

    private val logger = KotlinLogging.logger {}

    override fun saveSearchRequest(request: SearchRequest): SearchRequest {
        val id = request.id
        val step1 = measureTimeMillis {
            repository.save(request) ?: throw DataNotSavedException()
        }
        logger.info { "saveSearchRequest step1: $step1" }

        if (id > 0) {
            var relatedOfferSearches = emptyList<OfferSearch>()

            val step2 = measureTimeMillis {
                relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)
                    .filter {
                        it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT
                    }
            }
            logger.info { "saveSearchRequest step2: $step2" }

            val step3 = measureTimeMillis {
                offerSearchRepository.delete(relatedOfferSearches)
            }
            logger.info { "saveSearchRequest step3: $step3" }
        }

        return request
    }

    override fun deleteSearchRequest(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)

            relatedOfferSearches = relatedOfferSearches.filter {
                it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT
            }

            offerSearchRepository.delete(relatedOfferSearches)

            return id
        }

        return 0
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
                    offerSearch.offerId,
                    OfferResultAction.NONE,
                    offerSearch.info,
                    ArrayList()
                )
                toBeSavedOfferSearched.add(newOfferSearch)
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
