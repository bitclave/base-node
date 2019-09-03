package com.bitclave.node.repository.request

import com.bitclave.node.repository.entities.RequestData

interface RequestDataRepository {

    fun getByFrom(from: String): List<RequestData>

    fun getByTo(to: String): List<RequestData>

    fun getByFromAndTo(from: String, to: String): List<RequestData>

    fun getByFromAndToAndRequestData(from: String, to: String, requestData: String): RequestData?

    fun getByRequestDataAndRootPk(requestData: String, rootPk: String): List<RequestData>

    fun findById(id: Long): RequestData?

    fun updateData(request: RequestData): RequestData

    fun saveAll(requests: List<RequestData>): List<RequestData>

    fun deleteByFromAndTo(publicKey: String)

    fun deleteByIds(ids: List<Long>)
}
