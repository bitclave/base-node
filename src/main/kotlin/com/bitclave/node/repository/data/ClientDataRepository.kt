package com.bitclave.node.repository.data

interface ClientDataRepository {

    fun getData(id: String): Map<String, String>

    fun updateData(id: String, data: Map<String, String>)

}
