package com.bitclave.node.repository.data

import com.bitclave.node.repository.models.ClientData
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.stereotype.Component

@Component
class PostgresClientDataRepositoryImpl(val repository: ClientDataCrudRepository) :
        ClientDataRepository {

    override fun getData(publicKey: String): Map<String, String> {
        return repository.findOne(publicKey)?.data ?: emptyMap()
    }

    override fun updateData(publicKey: String, data: Map<String, String>) {
        repository.save(ClientData(publicKey, data)) ?: throw DataNotSaved()
    }

}
