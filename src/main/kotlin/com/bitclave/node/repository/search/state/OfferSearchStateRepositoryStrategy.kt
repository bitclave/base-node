package com.bitclave.node.repository.search.state

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferSearchStateRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresOfferSearchStateRepositoryImpl

) : RepositoryStrategy<OfferSearchStateRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferSearchStateRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }
}
