package com.bitclave.node.repository.rank

import com.bitclave.node.repository.models.OfferRank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component



@Component
@Qualifier("postgres")
class PostgresOfferRankRepositoryImpl(
        val repository: OfferRankCrudRepository
): OfferRankRepository {
    override fun saveRankOffer(rankOffer: OfferRank): OfferRank {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteRankOffer(id: Long): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
