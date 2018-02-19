package com.bitclave.node.repository.request

import com.bitclave.node.repository.models.RequestData

interface RequestDataRepository {

    fun getByFrom(from: String, state: RequestData.RequestDataState): List<RequestData>

    fun getByTo(to: String, state: RequestData.RequestDataState): List<RequestData>

    fun getByFromAndTo(
            from: String,
            to: String,
            state: RequestData.RequestDataState
    ): List<RequestData>

    fun findById(id: Long): RequestData?

    fun updateData(request: RequestData): RequestData

}
