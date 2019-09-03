package com.bitclave.node.repository.request

import com.bitclave.node.repository.entities.RequestData
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface RequestDataCrudRepository : CrudRepository<RequestData, Long> {

    fun findByFromPk(from: String): List<RequestData>

    fun findByToPk(to: String): List<RequestData>

    fun findByFromPkAndToPk(from: String, to: String): List<RequestData>

    fun findByFromPkAndToPkAndRequestData(from: String, to: String, requestData: String): RequestData?

    fun findByRequestDataAndRootPk(requestData: String, rootPk: String): List<RequestData>

    fun deleteByIdIn(ids: List<Long>)
}
