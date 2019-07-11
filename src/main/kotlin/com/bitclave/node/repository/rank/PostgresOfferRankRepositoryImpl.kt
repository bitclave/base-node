package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferRankRepositoryImpl(
    val repository: OfferRankCrudRepository
) : OfferRankRepository {

    override fun findByOfferIdAndRankerId(offerId: Long, rankerId: String): OfferRank? {
        return repository.findByOfferIdAndRankerId(offerId, rankerId)
    }

    override fun findByOfferId(id: Long): List<OfferRank> {
        return repository.findByOfferId(id)
    }

    override fun saveRankOffer(rankOffer: OfferRank): OfferRank {
        return repository.save(rankOffer)
    }

    override fun findById(id: Long): OfferRank? {
        return repository.findById(id)
    }

    override fun deleteByOfferIdIn(offerIds: List<Long>): Int {
        if (offerIds.isEmpty()) return 0
        return repository.deleteByOfferIdIn(offerIds)
    }
}
