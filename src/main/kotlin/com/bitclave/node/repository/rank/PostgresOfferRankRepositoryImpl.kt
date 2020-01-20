package com.bitclave.node.repository.rank

import com.bitclave.node.repository.entities.OfferRank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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

    @Transactional(readOnly = true)
    override fun findById(id: Long): OfferRank? {
        return repository.findByIdOrNull(id)
    }

    override fun deleteByOfferIdIn(offerIds: List<Long>): Int {
        if (offerIds.isEmpty()) return 0
        return repository.deleteByOfferIdIn(offerIds)
    }
}
