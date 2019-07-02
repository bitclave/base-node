package com.bitclave.node.repository.search.interaction

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferInteractionRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresOfferInteractionRepositoryImpl

) : RepositoryStrategy<OfferInteractionRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferInteractionRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }
}
