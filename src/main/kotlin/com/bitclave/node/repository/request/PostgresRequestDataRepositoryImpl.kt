package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
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

    override fun getByFromAndTo(from: String, to: String): List<RequestData> {
        return repository.findByFromPkAndToPk(from, to)
    }

    override fun getByFromAndToAndRequestData(from: String, to: String, requestData: String): RequestData? {
        return repository.findByFromPkAndToPkAndRequestData(from, to, requestData)
    }

    override fun getByRequestDataAndRootPk(requestData: String, rootPk: String): List<RequestData> {
        return repository.findByRequestDataAndRootPk(requestData, rootPk)
    }

    override fun findById(id: Long): RequestData? {
        return repository.findOne(id)
    }

    override fun updateData(request: RequestData): RequestData {
        return repository.save(request) ?: throw DataNotSavedException()
    }

    override fun saveAll(requests: List<RequestData>): List<RequestData> {
        return repository.save(requests).toList()
    }

    override fun deleteByFromAndTo(publicKey: String) {
        repository.delete(repository.findByFromPk(publicKey))
        repository.delete(repository.findByToPk(publicKey))
    }

    override fun deleteByIds(ids: List<Long>) {
        repository.deleteByIdIn(ids)
    }
}
