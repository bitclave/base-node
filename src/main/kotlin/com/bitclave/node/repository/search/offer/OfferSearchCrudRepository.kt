package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.models.OfferSearch
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchCrudRepository : PagingAndSortingRepository<OfferSearch, Long> {

    fun findBySearchRequestId(id: Long): List<OfferSearch>

}
