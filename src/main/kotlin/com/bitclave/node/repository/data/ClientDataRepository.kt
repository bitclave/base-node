package com.bitclave.node.repository.data

interface ClientDataRepository {

    fun getData(publicKey: String): Map<String, String>

    fun updateData(publicKey: String, data: Map<String, String>)

}
