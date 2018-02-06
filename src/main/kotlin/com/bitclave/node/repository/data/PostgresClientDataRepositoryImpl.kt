package com.bitclave.node.repository.data

import com.bitclave.node.repository.models.ClientData
import org.springframework.stereotype.Component

@Component
class PostgresClientDataRepositoryImpl(val repository: ClientDataCrudRepository) :
        ClientDataRepository {

    override fun getData(id: String): Map<String, String> {
        return repository.findOne(id)?.data ?: emptyMap<String, String>();
    }

    override fun updateData(id: String, data: Map<String, String>) {
        repository.save(ClientData(id, data)) ?: throw RuntimeException("data not saved")
    }

}
