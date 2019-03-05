package com.bitclave.node.repository.file

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class FileRepositoryStrategy(
    @Qualifier("postgres")
    private val postgres: PostgresFileRepositoryImpl

) : RepositoryStrategy<FileRepository> {

    override fun changeStrategy(type: RepositoryStrategyType): FileRepository {
        return when (type) {
            RepositoryStrategyType.POSTGRES -> postgres
            RepositoryStrategyType.HYBRID -> postgres
        }
    }
}
