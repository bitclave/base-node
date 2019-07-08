package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.offer.OfferSearchCrudRepository
import com.bitclave.node.services.errors.DataNotSavedException
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

    override fun save(request: SearchRequest): SearchRequest =
        repository.save(request) ?: throw DataNotSavedException()

    override fun save(request: List<SearchRequest>): List<SearchRequest> = repository.save(request).toList()

    override fun deleteByIdAndOwner(id: Long, owner: String): Long = repository.deleteByIdAndOwner(id, owner)

    override fun deleteByOwner(owner: String): Long {
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
        offerSearchRepository.delete(relatedOfferSearches)
    }
}
