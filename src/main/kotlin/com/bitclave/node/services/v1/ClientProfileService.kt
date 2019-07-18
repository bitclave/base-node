package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.data.ClientDataRepository
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
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

        return supplyAsyncEx(Supplier {
            if ("first" == publicKey) {
                return@Supplier hashMapOf("firstName" to "Adam", "lastName" to "Base")
            }

            clientDataRepository.changeStrategy(strategy)
                .getData(publicKey)
                .filter { includeOnly.isEmpty() || includeOnly.contains(it.key) }
        })
    }

    fun updateData(
        publicKey: String,
        data: Map<String, String>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Map<String, String>> {

        return supplyAsyncEx(Supplier {
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

        return runAsyncEx(Runnable {
            clientDataRepository.changeStrategy(strategy)
                .deleteData(publicKey)
        })
    }
}
