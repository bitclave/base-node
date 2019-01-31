package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
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

}
