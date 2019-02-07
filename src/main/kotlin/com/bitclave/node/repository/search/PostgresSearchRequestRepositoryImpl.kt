package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DataNotSavedException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
        val repository: SearchRequestCrudRepository,
        val offerSearchRepository: OfferSearchCrudRepository
) : SearchRequestRepository {

    override fun saveSearchRequest(request: SearchRequest): SearchRequest {
        var id = request.id;
        repository.save(request) ?: throw DataNotSavedException()

        if (id > 0) {
            var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)

            relatedOfferSearches = relatedOfferSearches.filter {
                it.state == OfferResultAction.NONE || it.state == OfferResultAction.REJECT
            }

            offerSearchRepository.delete(relatedOfferSearches)
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

    override fun findByOwner(owner: String): List<SearchRequest> {
        return repository.findByOwner(owner)
    }

    override fun findByIdAndOwner(id: Long, owner: String): SearchRequest? {
        return repository.findByIdAndOwner(id, owner)
    }

    override fun findAll(): List<SearchRequest> {
        return repository.findAll()
                .asSequence()
                .toList()
    }

     override fun cloneSearchRequestWithOfferSearches(request: SearchRequest): SearchRequest {
        var existingRequest = repository.findOne(request.id)
        if(existingRequest == null) return throw BadArgumentException("SearchRequest does not exist: " + request.id.toString())

        var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(existingRequest.id)

        val createSearchRequest = SearchRequest(
                0,
                request.owner,
                request.tags
        )

        repository.save(createSearchRequest)

        var toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf()
        for (offerSearch: OfferSearch in relatedOfferSearches) {
            val newOfferSearch = OfferSearch(
                    0,
                    createSearchRequest.owner,
                    createSearchRequest.id,
                    offerSearch.offerId,
                    OfferResultAction.NONE,
                    offerSearch.lastUpdated,
                    offerSearch.info,
                    ArrayList()
            )
            toBeSavedOfferSearched.add(newOfferSearch)
        }

        offerSearchRepository.save(toBeSavedOfferSearched)

        return createSearchRequest
    }

    override fun findAll(pageable: Pageable): Page<SearchRequest> {
        return repository.findAll(pageable)
    }
}
