package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank

interface OfferRankRepository {
    fun saveRankOffer(rankOffer: OfferRank): OfferRank
    fun deleteRankOffer(id: Long): Long
}
