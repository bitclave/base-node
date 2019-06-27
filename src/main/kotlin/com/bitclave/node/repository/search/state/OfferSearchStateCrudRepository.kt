package com.bitclave.node.repository.search.state

import com.bitclave.node.repository.models.OfferSearchState
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferSearchStateCrudRepository : PagingAndSortingRepository<OfferSearchState, Long> {

    fun findByOfferIdAndOwner(offerId: Long, owner: String): OfferSearchState?

    fun findByOfferIdInAndOwnerIn(offerIds: List<Long>, owners: List<String>): List<OfferSearchState>

    fun findByOfferIdInAndOwner(offerIds: List<Long>, owner: String): List<OfferSearchState>

    fun findByOfferId(offerId: Long): List<OfferSearchState>

    fun findByOfferIdIn(offerIds: List<Long>): List<OfferSearchState>

    fun deleteAllByOwner(owner: String): Long

    fun deleteByIdIn(ids: List<Long>): Long
}
