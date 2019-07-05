package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferRankCrudRepository : PagingAndSortingRepository<OfferRank, Long> {

    fun findByOfferId(offerId: Long): List<OfferRank>

    fun findByOfferIdAndRankerId(offerId: Long, rankerId: String): OfferRank?

    fun findById(id: Long): OfferRank?

    fun deleteByOfferIdIn(offerIds: List<Long>): Long
}
