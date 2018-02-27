package com.bitclave.node.repository.account

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AccountRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresAccountRepositoryImpl,

        @Qualifier("hybrid")
        private val hybrid: HybridAccountRepositoryImpl

) : RepositoryStrategy<AccountRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): AccountRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> hybrid
        }
    }

}
