package com.bitclave.node.repository.offer

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresOfferRepositoryImpl

) : RepositoryStrategy<OfferRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }

}
