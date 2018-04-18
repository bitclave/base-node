package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.request.RequestDataRepository
import com.bitclave.node.services.errors.BadArgumentException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class RequestDataService(private val requestDataRepository: RepositoryStrategy<RequestDataRepository>) {

    fun getRequestByStatus(
            fromPk: String?,
            toPk: String?,
            state: RequestData.RequestDataState,
            strategy: RepositoryStrategyType
    ): CompletableFuture<List<RequestData>> {

        return CompletableFuture.supplyAsync({
            val result: List<RequestData> =
                    if (fromPk == null && toPk != null) {
                        requestDataRepository.changeStrategy(strategy)
                                .getByTo(toPk, state)

                    } else if (fromPk != null && toPk == null) {
                        requestDataRepository.changeStrategy(strategy)
                                .getByFrom(fromPk, state)

                    } else if (fromPk != null && toPk != null) {
                        requestDataRepository.changeStrategy(strategy)
                                .getByFromAndTo(fromPk, toPk, state)

                    } else {
                        throw BadArgumentException()
                    }

            result
        })
    }

    fun request(clientPk: String, data: RequestData, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            val request = RequestData(
                    0L,
                    clientPk,
                    data.toPk,
                    data.requestData,
                    "",
                    RequestData.RequestDataState.AWAIT
            )
            requestDataRepository.changeStrategy(strategy)
                    .updateData(request).id
        })
    }

    fun response(
            id: Long,
            publicKey: String,
            data: String?,
            strategy: RepositoryStrategyType
    ): CompletableFuture<RequestData.RequestDataState> {

        return CompletableFuture.supplyAsync({
            val original = requestDataRepository.changeStrategy(strategy)
                    .findById(id)

            if (original == null || original.toPk != publicKey) {
                throw BadArgumentException()
            }

            val state = when {
                data.isNullOrEmpty() -> RequestData.RequestDataState.REJECT
                else -> RequestData.RequestDataState.ACCEPT
            }

            val result = RequestData(
                    original.id,
                    original.fromPk,
                    original.toPk,
                    original.requestData,
                    data ?: "",
                    state
            )

            requestDataRepository.changeStrategy(strategy)
                    .updateData(result)

            state
        })
    }

    fun grantAccess(
            clientId: String,
            data: RequestData,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync({
            if (data.responseData.isEmpty() || data.toPk != clientId || data.fromPk.length < 66) {
                throw BadArgumentException()
            }

            val request = RequestData(
                    0L,
                    data.fromPk,
                    clientId,
                    "",
                    data.responseData,
                    RequestData.RequestDataState.ACCEPT
            )

            requestDataRepository.changeStrategy(strategy)
                    .updateData(request).id
        })
    }

    fun deleteRequestsAndResponses(
            publicKey: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync({
            requestDataRepository.changeStrategy(strategy)
                    .deleteByFromAndTo(publicKey)
        })
    }

}
