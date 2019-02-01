package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferSearch
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
        val repository: SearchRequestCrudRepository,
        val offerSearchRepository: OfferSearchCrudRepository
) : SearchRequestRepository {

    override fun saveSearchRequest(request: SearchRequest): SearchRequest {
        return repository.save(request) ?: throw DataNotSavedException()
    }

    override fun deleteSearchRequest(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(id)

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
        if(existingRequest == null) return throw NotFoundException()

        var relatedOfferSearches = offerSearchRepository.findBySearchRequestId(existingRequest.id)

        val createSearchRequest = SearchRequest(
                0,
                request.owner,
                request.tags
        )

        repository.save(createSearchRequest)

        var toBeSavedOfferSearched: MutableList<OfferSearch> = mutableListOf<OfferSearch>()
        for (offerSearch: OfferSearch in relatedOfferSearches) {
            val newOfferSearch = OfferSearch(0, createSearchRequest.id,
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

}
