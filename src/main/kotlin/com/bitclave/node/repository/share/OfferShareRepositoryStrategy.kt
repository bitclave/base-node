package com.bitclave.node.repository.share

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferShareRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresOfferShareRepositoryImpl

) : RepositoryStrategy<OfferShareRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferShareRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }

}
