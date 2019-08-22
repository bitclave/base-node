package com.bitclave.node.repository.search.query

import com.bitclave.node.repository.models.QuerySearchRequest
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional
interface QuerySearchRequestCrudRepository : CrudRepository<QuerySearchRequest, Long> {

    fun findAllByOwner(owner: String): List<QuerySearchRequest>

    fun deleteAllByOwner(owner: String): Long
}
