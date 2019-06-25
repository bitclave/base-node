package com.bitclave.node.services.v1

import com.bitclave.node.BaseNodeApplication
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.data.ClientDataRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class ClientProfileService(
    private val clientDataRepository: RepositoryStrategy<ClientDataRepository>
) {

    fun getData(
        publicKey: String,
        includeOnly: Set<String>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Map<String, String>> {

        return CompletableFuture.supplyAsync(Supplier {
            if ("first" == publicKey) {
                return@Supplier hashMapOf("firstName" to "Adam", "lastName" to "Base")
            }

            clientDataRepository.changeStrategy(strategy)
                .getData(publicKey)
                .filter { includeOnly.isEmpty() || includeOnly.contains(it.key) }
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun updateData(
        publicKey: String,
        data: Map<String, String>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Map<String, String>> {

        return CompletableFuture.supplyAsync(Supplier {
            val oldData = clientDataRepository.changeStrategy(strategy)
                .getData(publicKey)
                .toMutableMap()

            oldData.putAll(data)

            clientDataRepository.changeStrategy(strategy)
                .updateData(publicKey, oldData)

            data
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun deleteData(
        publicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync(Runnable {
            clientDataRepository.changeStrategy(strategy)
                .deleteData(publicKey)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }
}
