package com.bitclave.node.repository.request

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.models.RequestData
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class RequestDataRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresRequestDataRepositoryImpl

) : RepositoryStrategy, RequestDataRepository {

    private var repository: RequestDataRepository = postgres

    override fun changeStrategy(type: RepositoryType) {
        repository = when (type) {
            RepositoryType.POSTGRES -> postgres
            RepositoryType.ETHEREUM -> postgres
        }
    }

    override fun getByFrom(from: String, state: RequestData.RequestDataState): List<RequestData> =
            repository.getByFrom(from, state)

    override fun getByTo(to: String, state: RequestData.RequestDataState): List<RequestData> =
            repository.getByTo(to, state)

    override fun getByFromAndTo(from: String, to: String,
                                state: RequestData.RequestDataState): List<RequestData> =
            repository.getByFromAndTo(from, to, state)

    override fun findById(id: Long): RequestData? = repository.findById(id)

    override fun updateData(request: RequestData): RequestData = repository.updateData(request)

}
