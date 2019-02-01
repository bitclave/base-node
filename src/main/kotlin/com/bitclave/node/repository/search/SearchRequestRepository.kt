package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest

interface SearchRequestRepository {

    fun saveSearchRequest(request: SearchRequest): SearchRequest

    fun deleteSearchRequest(id: Long, owner: String): Long

    fun deleteSearchRequests(owner: String): Long

    fun findById(id: Long): SearchRequest?

    fun findByOwner(owner: String): List<SearchRequest>

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

    fun findAll(): List<SearchRequest>

    fun cloneSearchRequestWithOfferSearches(request: SearchRequest): SearchRequest

}
