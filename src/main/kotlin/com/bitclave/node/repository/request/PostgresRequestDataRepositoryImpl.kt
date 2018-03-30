package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresRequestDataRepositoryImpl(val repository: RequestDataCrudRepository) :
        RequestDataRepository {

    override fun getByFrom(from: String, state: RequestData.RequestDataState): List<RequestData> {
        return repository.findByFromPkAndState(from, state)
    }

    override fun getByTo(to: String, state: RequestData.RequestDataState): List<RequestData> {
        return repository.findByToPkAndState(to, state)
    }

    override fun getByFromAndTo(
            from: String,
            to: String,
            state: RequestData.RequestDataState
    ): List<RequestData> {

        return repository.findByFromPkAndToPkAndState(from, to, state)
    }

    override fun findById(id: Long): RequestData? {
        return repository.findOne(id)
    }

    override fun updateData(request: RequestData): RequestData {
        return repository.save(request) ?: throw DataNotSaved()
    }

    override fun deleteAccount(publicKey: String): Long
    {
        for (l in repository.findByFromPk(publicKey))
            repository.delete(l);

        for (l in repository.findByToPk(publicKey))
            repository.delete(l);

        return 1L
    }


}
