package com.bitclave.node.repository.data

interface ClientDataRepository {

    fun allKeys(): Array<String>

    fun getData(publicKey: String): Map<String, String>

    fun updateData(publicKey: String, data: Map<String, String>)

    fun deleteData(publicKey: String)

}
