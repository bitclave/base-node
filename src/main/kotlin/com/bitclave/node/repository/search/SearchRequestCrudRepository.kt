package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface SearchRequestCrudRepository : PagingAndSortingRepository<SearchRequest, Long> {

    fun findById(id: Long): List<SearchRequest>

    fun findByOwner(owner: String): List<SearchRequest>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): Long

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

}
