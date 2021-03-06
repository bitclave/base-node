package com.bitclave.node.repository.rank

import com.bitclave.node.repository.entities.OfferRank

interface OfferRankRepository {

    fun saveRankOffer(rankOffer: OfferRank): OfferRank

    fun findById(id: Long): OfferRank?

    fun findByOfferId(id: Long): List<OfferRank>

    fun findByOfferIdAndRankerId(offerId: Long, rankerId: String): OfferRank?

    fun deleteByOfferIdIn(offerIds: List<Long>): Int
}
