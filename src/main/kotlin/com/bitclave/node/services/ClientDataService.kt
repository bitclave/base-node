package com.bitclave.node.services

import com.bitclave.node.repository.data.ClientDataRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ClientDataService(private val clientDataRepository: ClientDataRepository) {

    fun getData(id: String): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            if ("first" == id) {
                return@supplyAsync hashMapOf("firstName" to "Adam", "lastName" to "Base")
            }

            clientDataRepository.getData(id)
        })
    }

    fun updateData(id: String, data: Map<String, String>): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            clientDataRepository.updateData(id, data)
            data
        })
    }

}
