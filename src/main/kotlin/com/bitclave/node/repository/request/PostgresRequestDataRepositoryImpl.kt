package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresRequestDataRepositoryImpl(val repository: RequestDataCrudRepository) :
        RequestDataRepository {

    override fun getByFrom(from: String): List<RequestData> {
        return repository.findByFromPk(from)
    }

    override fun getByTo(to: String): List<RequestData> {
        return repository.findByToPk(to)
    }

    override fun getByFromAndTo(from: String, to: String): RequestData? {
        return repository.findByFromPkAndToPk(from, to)
    }

    override fun findById(id: Long): RequestData? {
        return repository.findByIdOrNull(id)
    }

    override fun updateData(request: RequestData): RequestData {
        return repository.save(request) ?: throw DataNotSavedException()
    }

    override fun deleteByFromAndTo(publicKey: String) {
        repository.deleteAll(repository.findByFromPk(publicKey))
        repository.deleteAll(repository.findByToPk(publicKey))
    }
}
