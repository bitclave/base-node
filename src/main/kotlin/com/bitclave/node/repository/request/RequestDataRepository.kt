package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData

interface RequestDataRepository {

    fun getByFrom(from: String): List<RequestData>

    fun getByTo(to: String): List<RequestData>

    fun getByFromAndTo(
            from: String,
            to: String
    ): RequestData?

    fun findById(id: Long): RequestData?

    fun updateData(request: RequestData): RequestData

    fun deleteByFromAndTo(publicKey: String)

}
