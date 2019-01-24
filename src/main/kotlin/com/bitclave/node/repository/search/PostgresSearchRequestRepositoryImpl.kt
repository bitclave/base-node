package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresSearchRequestRepositoryImpl(
        val repository: SearchRequestCrudRepository
) : SearchRequestRepository {

    override fun saveSearchRequest(request: SearchRequest): SearchRequest {
        return repository.save(request) ?: throw DataNotSavedException()
    }

    override fun deleteSearchRequest(id: Long, owner: String): Long {
        val count = repository.deleteByIdAndOwner(id, owner)
        if (count > 0) {
            return id
        }

        return 0
    }

    override fun deleteSearchRequests(owner: String): Long {
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

    override fun findAll(pageable: Pageable): Page<SearchRequest> {
        return repository.findAll(pageable)
    }
}
