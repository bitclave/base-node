package com.bitclave.node.repository.data

import com.bitclave.node.repository.models.ClientData
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresClientDataRepositoryImpl(
        val repository: ClientDataCrudRepository
) : ClientDataRepository {

    override fun allKeys(): Array<String> {
        return emptyArray()
    }

    override fun getData(publicKey: String): Map<String, String> {
        return repository.findOne(publicKey)?.data ?: emptyMap()
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        repository.save(ClientData(publicKey, data)) ?: throw DataNotSaved()
    }

    override fun deleteAccount(publicKey: String): Long
    {
        if (repository.exists(publicKey)) repository.delete(publicKey);
        return 1L
    }
}
