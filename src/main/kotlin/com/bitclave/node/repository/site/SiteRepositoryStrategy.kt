package com.bitclave.node.repository.site

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class SiteRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresSiteRepositoryImpl

) : RepositoryStrategy<SiteRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): SiteRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }

}
