package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.data.ClientDataRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
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
            val oldData = clientDataRepository.changeStrategy(strategy)
                    .getData(publicKey)
                    .toMutableMap()

            oldData.putAll(data)

            clientDataRepository.changeStrategy(strategy)
                    .updateData(publicKey, oldData)

            data
        })
    }

    fun deleteData(
            publicKey: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync({
            clientDataRepository.changeStrategy(strategy)
                    .deleteData(publicKey)
        })
    }

}
