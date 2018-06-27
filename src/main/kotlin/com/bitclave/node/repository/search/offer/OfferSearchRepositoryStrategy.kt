package com.bitclave.node.repository.search.offer

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferSearchRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresOfferSearchRepositoryImpl

) : RepositoryStrategy<OfferSearchRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferSearchRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }

}
