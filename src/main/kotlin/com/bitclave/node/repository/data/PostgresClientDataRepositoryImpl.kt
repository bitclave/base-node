package com.bitclave.node.repository.data

import com.bitclave.node.repository.entities.ClientData
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresClientDataRepositoryImpl(
    private val repository: ClientDataCrudRepository
) : ClientDataRepository {

    override fun allKeys(): List<String> {
        return emptyList()
    }

    override fun getData(publicKey: String): Map<String, String> {
        return repository.findByIdOrNull(publicKey)?.data ?: emptyMap()
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        repository.save(ClientData(publicKey, data)) ?: throw DataNotSavedException()
    }

    override fun deleteData(publicKey: String) {
        repository.deleteByPublicKey(publicKey)
    }
}
