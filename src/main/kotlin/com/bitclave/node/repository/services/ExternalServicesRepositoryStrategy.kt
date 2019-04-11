package com.bitclave.node.repository.services

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ExternalServicesRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresExternalServicesRepositoryImpl
) : RepositoryStrategy<ExternalServicesRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): ExternalServicesRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }
}
