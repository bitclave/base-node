package com.bitclave.node.repository.data

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ClientDataRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresClientDataRepositoryImpl

) : RepositoryStrategy, ClientDataRepository {

    private var repository: ClientDataRepository = postgres

    override fun changeStrategy(type: RepositoryType) {
        repository = when (type) {
            RepositoryType.POSTGRES -> postgres
            RepositoryType.ETHEREUM -> postgres
        }
    }

    override fun getData(publicKey: String): Map<String, String> = repository.getData(publicKey)


    override fun updateData(publicKey: String, data: Map<String, String>) =
            repository.updateData(publicKey, data)

}
