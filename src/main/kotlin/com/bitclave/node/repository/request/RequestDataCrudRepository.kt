package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface RequestDataCrudRepository : CrudRepository<RequestData, Long> {

    fun findByFromPk(from: String): List<RequestData>

    fun findByToPk(to: String): List<RequestData>

    fun findByFromPkAndToPk(from: String, to: String): RequestData?

}
