package com.bitclave.node.repository.rank

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferRankRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresOfferRankRepositoryImpl
) : RepositoryStrategy<OfferRankRepository> {
    override fun changeStrategy(type: RepositoryStrategyType): OfferRankRepository {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
