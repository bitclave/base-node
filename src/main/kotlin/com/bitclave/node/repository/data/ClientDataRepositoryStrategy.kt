package com.bitclave.node.repository.data

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ClientDataRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresClientDataRepositoryImpl,

        @Qualifier("hybrid")
        private val hybrid: HybridClientDataRepositoryImpl

) : RepositoryStrategy<ClientDataRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): ClientDataRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> hybrid
        }
    }

}
