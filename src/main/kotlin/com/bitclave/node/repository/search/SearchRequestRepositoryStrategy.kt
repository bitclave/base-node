package com.bitclave.node.repository.search

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class SearchRequestRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresSearchRequestRepositoryImpl

) : RepositoryStrategy<SearchRequestRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): SearchRequestRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }

}
