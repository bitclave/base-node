package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component



@Component
@Qualifier("postgres")
class PostgresOfferRankRepositoryImpl(
        val repository: OfferRankCrudRepository
): OfferRankRepository {
    override fun findByOfferId(id: Long): List<OfferRank> {
        return repository.findByOfferId(id) //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveRankOffer(rankOffer: OfferRank): OfferRank {
        return repository.save(rankOffer)
    }

    override fun findById(id: Long): OfferRank? {
        return repository.findById(id)
    }
}
