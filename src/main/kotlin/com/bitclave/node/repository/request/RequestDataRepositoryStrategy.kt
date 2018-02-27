package com.bitclave.node.repository.request

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("RequestDataRepository")
class RequestDataRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresRequestDataRepositoryImpl

) : RepositoryStrategy<RequestDataRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): RequestDataRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.ETHEREUM -> postgres
        }
    }

}
