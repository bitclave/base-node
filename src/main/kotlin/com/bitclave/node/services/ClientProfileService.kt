package com.bitclave.node.services

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.data.ClientDataRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ClientProfileService(
        private val clientDataRepository: RepositoryStrategy<ClientDataRepository>
) {

    fun getData(publicKey: String, strategy: RepositoryStrategyType): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            if ("first" == publicKey) {
                return@supplyAsync hashMapOf("firstName" to "Adam", "lastName" to "Base")
            }

            clientDataRepository.changeStrategy(strategy)
                    .getData(publicKey)
        })
    }

    fun updateData(
            publicKey: String,
            data: Map<String, String>,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Map<String, String>> {

        return CompletableFuture.supplyAsync({
            clientDataRepository.changeStrategy(strategy)
                    .updateData(publicKey, data)
            data
        })
    }

    fun deleteAccount(
            publicKey: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync({
            clientDataRepository.changeStrategy(strategy)
                    .deleteAccount(publicKey)
        })
    }

}
