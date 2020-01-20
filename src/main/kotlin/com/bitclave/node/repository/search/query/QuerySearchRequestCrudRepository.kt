package com.bitclave.node.repository.search.query

import com.bitclave.node.repository.entities.QuerySearchRequest
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface QuerySearchRequestCrudRepository : CrudRepository<QuerySearchRequest, Long> {

    @Transactional(readOnly = true)
    fun findAllByOwner(owner: String): List<QuerySearchRequest>

    fun deleteAllByOwner(owner: String): Long
}
