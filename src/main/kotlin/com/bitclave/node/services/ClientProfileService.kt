package com.bitclave.node.services

import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.data.ClientDataRepositoryStrategy
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ClientProfileService(
        private val clientDataRepository: ClientDataRepositoryStrategy
) {

    init {
        clientDataRepository.changeStrategy(RepositoryType.POSTGRES)
    }

    fun getData(publicKey: String): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            if ("first" == publicKey) {
                return@supplyAsync hashMapOf("firstName" to "Adam", "lastName" to "Base")
            }

            clientDataRepository.getData(publicKey)
        })
    }

    fun updateData(publicKey: String, data: Map<String, String>): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            clientDataRepository.updateData(publicKey, data)
            data
        })
    }

}
