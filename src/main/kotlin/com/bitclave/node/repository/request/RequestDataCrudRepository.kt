package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface RequestDataCrudRepository : CrudRepository<RequestData, Long> {

    fun findByFromPkAndState(from: String, state: RequestData.RequestDataState): List<RequestData>

    fun findByToPkAndState(to: String, state: RequestData.RequestDataState): List<RequestData>

    fun findByFromPkAndToPkAndState(
            from: String,
            to: String,
            state: RequestData.RequestDataState
    ): List<RequestData>

}
