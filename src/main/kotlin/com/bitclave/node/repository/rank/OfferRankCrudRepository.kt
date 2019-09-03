package com.bitclave.node.repository.rank

import com.bitclave.node.repository.entities.OfferRank
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferRankCrudRepository : PagingAndSortingRepository<OfferRank, Long> {

    fun findByOfferId(offerId: Long): List<OfferRank>

    fun findByOfferIdAndRankerId(offerId: Long, rankerId: String): OfferRank?

    @Modifying
    @Query(
        value = """
            DELETE FROM OfferRank r WHERE r.offerId IN ?1
        """
    )
    fun deleteByOfferIdIn(offerIds: List<Long>): Int
}
