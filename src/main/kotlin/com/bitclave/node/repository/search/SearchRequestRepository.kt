package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SearchRequestRepository {

    fun saveSearchRequest(request: SearchRequest): SearchRequest

    fun deleteSearchRequest(id: Long, owner: String): Long

    fun deleteSearchRequests(owner: String): Long

    fun findById(id: Long): SearchRequest?

    fun findByOwner(owner: String): List<SearchRequest>

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

    fun findAll(): List<SearchRequest>

    fun findAll(pageable: Pageable): Page<SearchRequest>
}
