package com.bitclave.node.repository.price

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class OfferPriceRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresOfferPriceRepositoryImpl
) : RepositoryStrategy<OfferPriceRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): OfferPriceRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }
}
