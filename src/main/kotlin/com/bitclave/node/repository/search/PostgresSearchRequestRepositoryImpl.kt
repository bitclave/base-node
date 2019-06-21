package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.OfferSearchState
import com.bitclave.node.repository.models.OfferSearchStateId
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

    override fun save(request: SearchRequest): SearchRequest {
        val hasId = request.id > 0
        val savedRequest = repository.save(request) ?: throw DataNotSavedException()

        if (hasId) {
            this.deleteRelevantOfferSearches(savedRequest.id)
        }

        return request
    }

    override fun deleteByIdAndOwner(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            this.deleteRelevantOfferSearches(id)
            return id
        }

        return 0
    }

    override fun deleteByOwner(owner: String): Long {
        // TODO deleteByIdAndOwner OfferSearch based on BULK deleted searchRequest
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

        val toBeSavedOfferSearched = relatedOfferSearches.map {
            it.copy(id = 0, owner = createSearchRequest.owner, searchRequestId = createSearchRequest.id)
        }

        val uniqueOwners = toBeSavedOfferSearched.map { it.owner }
        val uniqueOfferIds = toBeSavedOfferSearched.map { it.offerId }

        val states = offerSearchStateCrudRepository
            .findByOfferIdInAndOwnerIn(uniqueOfferIds, uniqueOwners)
            .groupBy { OfferSearchStateId(it.offerId, it.owner) }

        val stateForSave = toBeSavedOfferSearched.filter { states[OfferSearchStateId(it.offerId, it.owner)] != null }
            .map { OfferSearchState(0, it.owner, it.offerId) }

        offerSearchStateCrudRepository.save(stateForSave)

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

    private fun deleteRelevantOfferSearches(searchRequestId: Long) {
        val relatedOfferSearches = offerSearchRepository.findBySearchRequestId(searchRequestId)
            .filter { it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT }
        offerSearchRepository.delete(relatedOfferSearches)
    }
}
