package com.bitclave.node.repository.search

import com.bitclave.node.repository.models.SearchRequest
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface SearchRequestCrudRepository : CrudRepository<SearchRequest, Long> {

    fun findByOwner(owner: String): List<SearchRequest>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): Long

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

}
